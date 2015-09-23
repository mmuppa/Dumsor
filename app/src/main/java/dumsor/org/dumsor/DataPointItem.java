package dumsor.org.dumsor;


public class DataPointItem
{
    private String id;
    private String source;
    private String auth;
    private double lat;
    private double lon;
    private long timestamp;
    private long previous;
    private int power;

    public DataPointItem(final String auth, final String source, final double lat,
                         final double lon, final long timestamp, final long previous,
                         final int power)
    {
        this.auth = auth;
        this.source = source;
        this.lat = lat;
        this.lon = lon;
        this.timestamp = timestamp;
        this.previous = previous;
        this.power = power;
    }

    public String get_auth()
    {
        return auth;
    }

    public String get_source()
    {
        return source;
    }

    public double get_lat()
    {
        return lat;
    }

    public double get_lon()
    {
        return lon;
    }

    public long get_timestamp()
    {
        return timestamp;
    }

    public long get_previous()
    {
        return previous;
    }

    public int get_power()
    {
        return power;
    }
}