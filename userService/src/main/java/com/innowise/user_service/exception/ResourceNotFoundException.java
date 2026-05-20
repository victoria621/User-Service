package com.innowise.user_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ResourceNotFoundException extends RuntimeException  {
    public ResourceNotFoundException(String message) {
        super(message);
    }

}
