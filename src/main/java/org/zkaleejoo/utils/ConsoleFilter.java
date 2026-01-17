package org.zkaleejoo.utils;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class ConsoleFilter implements Filter {
    @Override
    public boolean isLoggable(LogRecord record) {
        if (record.getMessage() == null) return true;
        
        String msg = record.getMessage();
        
        return !msg.contains("ACCESS DENIED!") && 
               !msg.contains("KICKED FROM SERVER") &&
               !msg.contains("lost connection: Banned") &&
               !msg.contains("lost connection: Kicked");
    }
}