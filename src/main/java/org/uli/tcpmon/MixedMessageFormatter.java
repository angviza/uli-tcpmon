package org.uli.tcpmon;

import java.util.List;

public class MixedMessageFormatter implements MessageFormatter {
    @Override
    public List<String> format(byte[] message) {
        HexDump hexDump = new HexDump();
        List<String> formatted = hexDump.dumpToList(message);
        return formatted;
    }
}
