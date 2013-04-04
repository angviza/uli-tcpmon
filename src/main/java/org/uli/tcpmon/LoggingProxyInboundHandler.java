package org.uli.tcpmon;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.*;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingProxyInboundHandler extends SimpleChannelUpstreamHandler {
    Logger logger = LoggerFactory.getLogger(LoggingProxyInboundHandler.class);
    private final ClientSocketChannelFactory cf;
    private final String remoteHost;
    private final int remotePort;
    private final MessageFormatter messageFormatter;
    private volatile Channel outboundChannel;

    public LoggingProxyInboundHandler(ClientSocketChannelFactory cf, String remoteHost, int remotePort, MessageFormatter messageFormatter) {
        logger.info("->");
        this.cf = cf;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.messageFormatter = messageFormatter;
        logger.info("<-");
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info("->");
        // Suspend incoming traffic until connected to the remote host.
        final Channel inboundChannel = e.getChannel();
        inboundChannel.setReadable(false);
        // Start the connection attempt.
        ClientBootstrap cb = new ClientBootstrap(cf);
        cb.getPipeline().addLast("handler", new OutboundHandler(e.getChannel(), this.messageFormatter));
        ChannelFuture f = cb.connect(new InetSocketAddress(remoteHost, remotePort));
        outboundChannel = f.getChannel();
        f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    // Connection attempt succeeded:
                    // Begin to accept incoming traffic.
                    inboundChannel.setReadable(true);
                } else {
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });
        logger.info("<-");
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        logger.info("->");
        ChannelBuffer msg = (ChannelBuffer) e.getMessage();
        if (logger.isDebugEnabled()) {
            ByteBuffer bb = msg.toByteBuffer();
            List<String> lines = this.messageFormatter.format(bb.array());
            for (String l : lines) {
                logger.debug(" : >>> {}", l);
            }
        }
        outboundChannel.write(msg);
        logger.info("<-");
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info("->");
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
        logger.info("<-");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.info("->");
        logger.warn("Exception:", e.getCause());
        closeOnFlush(e.getChannel());
        logger.info("<-");
    }

    private static class OutboundHandler extends SimpleChannelUpstreamHandler {
        Logger logger = LoggerFactory.getLogger(OutboundHandler.class);
        private final Channel inboundChannel;
        private final MessageFormatter messageFormatter;

        OutboundHandler(Channel inboundChannel, MessageFormatter messageFormatter) {
            logger.info("->");
            this.inboundChannel = inboundChannel;
            this.messageFormatter = messageFormatter;
            logger.info("<-");
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            logger.info("->");
            ChannelBuffer msg = (ChannelBuffer) e.getMessage();
            if (logger.isDebugEnabled()) {
                ByteBuffer bb = msg.toByteBuffer();
                List<String> lines = this.messageFormatter.format(bb.array());
                for (String l : lines) {
                    logger.debug(" : >>> {}", l);
                }
            }
            inboundChannel.write(msg);
            logger.info("<-");
        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            logger.info("->");
            closeOnFlush(inboundChannel);
            logger.info("<-");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            logger.info("->");
            logger.warn("Exception:", e.getCause());
            closeOnFlush(e.getChannel());
            logger.info("<-");
        }
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        Logger logger = LoggerFactory.getLogger(LoggingProxyInboundHandler.class);
        logger.info("->");
        if (ch.isConnected()) {
            ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
        logger.info("<-");
    }
}
