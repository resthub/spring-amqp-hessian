package org.resthub.rpc.service;


public class FailingServiceImpl implements FailingService
{
    public void timeout(long time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Object getNotSerializable()
    {
        return this;
    }
}
