package mrtjp.projectred.transportation;

import java.util.concurrent.PriorityBlockingQueue;

public class TableUpdateThread extends Thread
{
    private static PriorityBlockingQueue<RouteLayerUpdater> updateCalls = new PriorityBlockingQueue<RouteLayerUpdater>();

    private static Long average = 0L;

    public TableUpdateThread(int i)
    {
        super("PR TableUpdateThread #" + i);
        setDaemon(true);
        setPriority(Thread.NORM_PRIORITY);
        start();
    }

    public static void add(Router r)
    {
        updateCalls.add(new RouteLayerUpdater(r));
    }

    public static boolean remove(Runnable run)
    {
        return updateCalls.remove(run);
    }

    public static int size()
    {
        return updateCalls.size();
    }

    public static long getAverage()
    {
        synchronized (average)
        {
            return average;
        }
    }

    @Override
    public void run()
    {
        RouteLayerUpdater rlu;
        try
        {
            while ((rlu = updateCalls.take()) != null)
            {
                long starttime = System.nanoTime();

                rlu.run();

                long took = System.nanoTime() - starttime;
                synchronized (average)
                {
                    if (average == 0)
                        average = took;
                    else
                        average = (average * 999L + took) / 1000L;
                }
            }
        } catch (InterruptedException e)
        {
        }
    }

    public static class RouteLayerUpdater implements Comparable<RouteLayerUpdater>, Runnable
    {
        int newVersion = 0;
        boolean complete = false;
        Router router;

        RouteLayerUpdater(Router router)
        {
            this.complete = false;
            this.newVersion = router.getLinkStateID();
            this.router = router;
        }

        @Override
        public void run()
        {
            if (complete)
                return;
            try
            {
                if (router.getParent() == null)
                    return;

                for (int i = 0; i < 10 && !router.isLoaded(); i++)
                    Thread.sleep(10);

                if (!router.isLoaded())
                    return;

                router.refreshRoutingTable(newVersion);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            complete = true;
        }

        @Override
        public int compareTo(RouteLayerUpdater o)
        {
            int c = 0;
            if (o.newVersion <= 0)
                c = newVersion - o.newVersion;

            if (c != 0)
                return c;

            c = router.getIPAddress() - o.router.getIPAddress();
            if (c != 0)
                return c;

            c = o.newVersion - newVersion;
            return c;
        }
    }
}
