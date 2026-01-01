package com.sshman.utils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Dates {

    public static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
}
