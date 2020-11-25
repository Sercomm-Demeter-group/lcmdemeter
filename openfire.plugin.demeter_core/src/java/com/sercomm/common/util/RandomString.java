package com.sercomm.common.util;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class RandomString
{
    /**
     * Generate a random string.
     */
    public String nextString() 
    {
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }

    public static final String SYMBOLS = "!@#$%&";
    public static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String LOWER = UPPER.toLowerCase(Locale.ROOT);
    public static final String DIGISTS = "0123456789";
    public static final String ALPHANUM = UPPER + LOWER + DIGISTS;
    public static final String BETANUM = SYMBOLS + UPPER + LOWER + DIGISTS;
    
    private final Random random;
    private final char[] symbols;
    private final char[] buf;

    public RandomString(int length, Random random, String symbols) 
    {
        if (length < 1) throw new IllegalArgumentException();
        if (symbols.length() < 2) throw new IllegalArgumentException();
        this.random = Objects.requireNonNull(random);
        this.symbols = symbols.toCharArray();
        this.buf = new char[length];
    }
    
    /**
     * Create an alphanumeric string generator.
     */
    public RandomString(int length, Random random) 
    {
        this(length, random, ALPHANUM);
    }

    /**
     * Create an alphanumeric strings from a secure generator.
     */
    public RandomString(int length) 
    {
        this(length, new SecureRandom());
    }

    /**
     * Create session identifiers.
     */
    public RandomString() 
    {
        this(21);
    }
}
