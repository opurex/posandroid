package com.opurex.client.utils.exception;

import java.io.IOException;

/**
 * Created by svirch_n on 22/01/16.
 */
public class CouldNotDisconnectException extends IOException {

    public CouldNotDisconnectException(Exception e) {
        super(e);
    }

    public CouldNotDisconnectException() {

    }

    public CouldNotDisconnectException(String s) {
        super(s);
    }

}
