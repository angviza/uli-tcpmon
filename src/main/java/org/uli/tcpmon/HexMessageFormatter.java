package org.uli.tcpmon;

import java.util.List;

public class HexMessageFormatter implements MessageFormatter {
    @Override
    public List<String> format(byte[] message) {
        HexDump hexDump = new HexDump(32, true, false);
        List<String> formatted = hexDump.dumpToList(message);
        return formatted;
    }
}
