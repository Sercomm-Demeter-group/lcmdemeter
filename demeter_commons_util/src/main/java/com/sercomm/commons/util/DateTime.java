package com.sercomm.commons.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

public class DateTime
{
    ////////////////////////////////////////////////////////////////////////////
	public static final String FORMAT_ISO =
            "yyyy-MM-dd'T'HH:mm:ssZ";
	public static final String FORMAT_ISO_MS =
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	
    public static final String FORMAT =
            "yyyy-MM-dd HH:mm:ss Z";
    public static final String FORMAT_MS =
            "yyyy-MM-dd HH:mm:ss.SSS Z";
    public static final String FORMAT_MYSQL =
            "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String FORMAT_CAMERA =
            "yyyyMMddHHmmssZ";
    public static final String REG_TIMEZONE =
            "(\\+|-)[0-9]{4}";
    public static final int SUNDAY =
        Calendar.SUNDAY;
    public static final int MONDAY =
        Calendar.SUNDAY;
    public static final int TUESDAY =
        Calendar.TUESDAY;
    public static final int WEDNESDAY =
        Calendar.WEDNESDAY;
    public static final int THURSDAY =
        Calendar.THURSDAY;
    public static final int FRIDAY =
        Calendar.FRIDAY;
    public static final int SATURDAY =
        Calendar.SATURDAY;
    ////////////////////////////////////////////////////////////////////////////
    private GregorianCalendar calendar =
        new GregorianCalendar();
    private SimpleDateFormat format =
        new SimpleDateFormat(FORMAT, Locale.US);
    ////////////////////////////////////////////////////////////////////////////
    public static DateTime now()
    {
        return new DateTime();
    }
    ////////////////////////////////////////////////////////////////////////////
    public static DateTime from(long unixTime)
    throws Exception
    {
        DateTime dateTime = new DateTime();
        dateTime.setTimeInMillis(unixTime);

        dateTime.setTimeZone(TimeZone.getDefault());
        return dateTime;
    }
    ////////////////////////////////////////////////////////////////////////////
    public static DateTime from(String timestamp)
    throws Exception
    {
        DateTime dateTime = new DateTime();
        dateTime.setDate(timestamp);
        
        Pattern pattern = Pattern.compile(REG_TIMEZONE);
        Matcher matcher = pattern.matcher(timestamp);
        if(false == matcher.find())
        {
            throw new Exception("can not find timezone");
        }

        dateTime.setTimeZone(TimeZone.getTimeZone("GMT" + matcher.group()));
        return dateTime;
    }
    ////////////////////////////////////////////////////////////////////////////
    public static DateTime from(String timestamp, String format)
    throws Exception
    {
        DateTime dateTime = new DateTime();
        dateTime.setDate(timestamp, format);
        
        Pattern pattern = Pattern.compile(REG_TIMEZONE);
        Matcher matcher = pattern.matcher(timestamp);
        if(false == matcher.find())
        {
            throw new Exception("can not find timezone");
        }

        dateTime.setTimeZone(TimeZone.getTimeZone("GMT" + matcher.group()));
        return dateTime;
    }
    ////////////////////////////////////////////////////////////////////////////
    private DateTime()
    {
    }
    ////////////////////////////////////////////////////////////////////////////
    public DateTime clone()
    {
        try 
        {
            return DateTime.from(this.toString());
        } 
        catch(Exception e) 
        {
            return null;
        }
    }
    ////////////////////////////////////////////////////////////////////////////
    public boolean setDate(
            int year, int month, int day, int hour, int minute, int second)
    {
        if(0 > year ||
           0 >= month && 12 < month ||
           0 >= day && 31 < day ||
           0 > hour && 24 < hour ||
           0 > minute && 60 < minute ||
           0 > second && 60 < second)
        {
            return false;
        }
        
        this.calendar.clear();
        this.calendar.set(year, month - 1, day, hour, minute, second);
        
        return true;
    }
    ////////////////////////////////////////////////////////////////////////////
    public boolean setDate(
            int year, int month, int day)
    {
        if(0 > year ||
           0 >= month && 12 < month ||
           0 >= day && 31 < day)
        {
            return false;
        }

        this.calendar.clear();
        this.calendar.set(year, month - 1, day, 0, 0, 0);
        return true;
    }
    ////////////////////////////////////////////////////////////////////////////
    public void setDate(String timestamp)
    throws ParseException
    {
        Date dateObject = this.format.parse(timestamp);
        this.calendar.clear();
        this.calendar.setTime(dateObject);
    }
    ////////////////////////////////////////////////////////////////////////////
    public void setDate(
            String timestamp,
            String format)
    throws ParseException
    {
        SimpleDateFormat dateFormat =
                new SimpleDateFormat(format, Locale.US);
        Date dateObject = dateFormat.parse(timestamp);
        
        this.calendar.clear();
        this.calendar.setTime(dateObject);
    }
    ////////////////////////////////////////////////////////////////////////////
    public String toString()
    {
        return this.format.format(this.calendar.getTime());
    }
    ////////////////////////////////////////////////////////////////////////////
    public String toString(String formatString)
    {
        SimpleDateFormat format = new SimpleDateFormat(formatString, Locale.US);
        format.setTimeZone(this.format.getTimeZone());
        
        return format.format(this.calendar.getTime());
    }
    ////////////////////////////////////////////////////////////////////////////
    public void setTimeZone(TimeZone timeZone)
    {
        this.format.setTimeZone(timeZone);
        this.calendar.setTimeZone(timeZone);
    }
    ////////////////////////////////////////////////////////////////////////////
    public boolean setTimeInMillis(
            long milliseconds)
    {
        if(milliseconds < 0)
        {
            return false;
        }
        
        this.calendar.clear();
        this.calendar.setTimeInMillis(milliseconds);
        
        return true;
    }
    ////////////////////////////////////////////////////////////////////////////
    public long getTimeInMillis()
    {
        return this.calendar.getTimeInMillis();
    }
    ////////////////////////////////////////////////////////////////////////////
    public long getTimeInSeconds()
    {
        return (long) (this.calendar.getTimeInMillis() / 1000L);
    }
    ////////////////////////////////////////////////////////////////////////////
    public String getTimeZone()
    {
        SimpleDateFormat format = new SimpleDateFormat("Z", Locale.US);
        format.setTimeZone(this.format.getTimeZone());
        
        return format.format(this.calendar.getTime());
    }
    ////////////////////////////////////////////////////////////////////////////
    public String getTimeZone(String formatString)
    {
        SimpleDateFormat format = new SimpleDateFormat(formatString, Locale.US);
        format.setTimeZone(this.format.getTimeZone());
        return format.format(this.calendar.getTime());
    }
    ////////////////////////////////////////////////////////////////////////////
    public int getYear()
    {
        return this.calendar.get(Calendar.YEAR);
    }
    ////////////////////////////////////////////////////////////////////////////
    public int getMonth()
    {
        return this.calendar.get(Calendar.MONTH) + 1;
    }
    ////////////////////////////////////////////////////////////////////////////
    public int getDay()
    {
        return this.calendar.get(Calendar.DAY_OF_MONTH);
    }
    ////////////////////////////////////////////////////////////////////////////
    public int getHour()
    {
        return this.calendar.get(Calendar.HOUR_OF_DAY);
    }
    ////////////////////////////////////////////////////////////////////////////
    public int getMinute()
    {
        return this.calendar.get(Calendar.MINUTE);
    }
    ////////////////////////////////////////////////////////////////////////////
    public int getSecond()
    {
        return this.calendar.get(Calendar.SECOND);
    }
    ////////////////////////////////////////////////////////////////////////////
    public int getMilliSecond()
    {
        return this.calendar.get(Calendar.MILLISECOND);
    }
    ////////////////////////////////////////////////////////////////////////////
    public int getWeekDay()
    {
        return this.calendar.get(Calendar.DAY_OF_WEEK);
    }
    ////////////////////////////////////////////////////////////////////////////
    public void addYear(int value)
    {
        this.calendar.add(Calendar.YEAR, value);
    }
    ////////////////////////////////////////////////////////////////////////////
    public void addMonth(int value)
    {
        this.calendar.add(Calendar.MONTH, value);
    }
    ////////////////////////////////////////////////////////////////////////////
    public void addDay(int value)
    {
        this.calendar.add(Calendar.DAY_OF_MONTH, value);
    }
    ////////////////////////////////////////////////////////////////////////////
    public void addHour(int value)
    {
        this.calendar.add(Calendar.HOUR_OF_DAY, value);
    }
    ////////////////////////////////////////////////////////////////////////////
    public void addMinute(int value)
    {
        this.calendar.add(Calendar.MINUTE, value);
    }
    ////////////////////////////////////////////////////////////////////////////
    public void addSecond(int value)
    {
        calendar.add(Calendar.SECOND, value);
    }
    ////////////////////////////////////////////////////////////////////////////
    public void addMilliSecond(int value)
    {
        this.calendar.add(Calendar.MILLISECOND, value);
    }
    ////////////////////////////////////////////////////////////////////////////
    public int compareTo(DateTime date)
    {
        return this.calendar.compareTo(date.calendar);
    }
    ////////////////////////////////////////////////////////////////////////////
    public long subDate(DateTime dateTarget)
    {
        long currentTime = (long)(this.calendar.getTimeInMillis() / 1000);
        long targetTime = (long)(dateTarget.calendar.getTimeInMillis() / 1000);
        return (targetTime - currentTime);
    }
    ////////////////////////////////////////////////////////////////////////////
    public int getDayOfYear()
    {
        return this.calendar.get(Calendar.DAY_OF_YEAR);
    }
    ////////////////////////////////////////////////////////////////////////////
    public int isUsingDST()
    {
        TimeZone timeZone = this.calendar.getTimeZone();
        return timeZone.inDaylightTime(this.calendar.getTime()) ? 1 : 0;
    }
    ////////////////////////////////////////////////////////////////////////////
}
