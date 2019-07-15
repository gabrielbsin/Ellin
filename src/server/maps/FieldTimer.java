package server.maps;
import client.Client;
import packet.transfer.write.OutPacket;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import packet.creators.PacketCreator;

public class FieldTimer {
    private final int duration;
    private final Calendar startTime;
    private final Calendar predictedStopTime;
    private int fieldToWarpTo = -1;
    private int minLevelToWarp = 0;
    private int maxLevelToWarp = 256;
    private final ScheduledFuture<?> schedule;

    public FieldTimer(ScheduledFuture<?> schedule, int newDuration, int fieldToWarpToP, int minLevelToWarpP, int maxLevelToWarpP) {
        this.duration = newDuration;
        this.startTime = Calendar.getInstance();
        this.predictedStopTime = Calendar.getInstance();
        this.predictedStopTime.add(Calendar.SECOND, duration);
        this.fieldToWarpTo = fieldToWarpToP;
        this.minLevelToWarp = minLevelToWarpP;
        this.maxLevelToWarp = maxLevelToWarpP;
        this.schedule = schedule;
    }

    public OutPacket makeSpawnData() {
        long StopTimeStamp = this.predictedStopTime.getTimeInMillis();
        long CurrentTimeStamp = Calendar.getInstance().getTimeInMillis();
        return PacketCreator.GetClockTimer((int) (StopTimeStamp - CurrentTimeStamp) / 1000);
    }

    public void sendSpawnData(Client c) {
        c.getSession().write(makeSpawnData());
    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public int warpToField() {
        return this.fieldToWarpTo;
    }

    public int minLevelToWarp() {
        return this.minLevelToWarp;
    }

    public int maxLevelToWarp() {
        return this.maxLevelToWarp;
    }

    public int getTimeLeft() {
        long StopTimeStamp = predictedStopTime.getTimeInMillis();
        long CurrentTimeStamp = Calendar.getInstance().getTimeInMillis();
        return (int) (StopTimeStamp - CurrentTimeStamp) / 1000;
    }
}
