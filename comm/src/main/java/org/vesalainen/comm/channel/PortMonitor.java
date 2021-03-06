/*
 * Copyright (C) 2017 Timo Vesalainen <timo.vesalainen@iki.fi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.vesalainen.comm.channel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.vesalainen.math.Sets;

/**
 *
 * @author Timo Vesalainen <timo.vesalainen@iki.fi>
 */
public class PortMonitor
{
    private ScheduledExecutorService scheduler;
    private long period;
    private TimeUnit unit;
    private List<Consumer<String>> newPortConsumers = new ArrayList<>();
    private List<Consumer<String>> newFreePortConsumers = new ArrayList<>();
    private List<Consumer<String>> removePortConsumers = new ArrayList<>();
    private List<Consumer<String>> removeFreePortConsumers = new ArrayList<>();
    private ScheduledFuture<?> future;
    private Set<String> ports = new HashSet<>();
    private Set<String> freePorts = new HashSet<>();

    public PortMonitor()
    {
        this(Executors.newSingleThreadScheduledExecutor(), 1, TimeUnit.MINUTES);
    }

    public PortMonitor(long period, TimeUnit unit)
    {
        this(Executors.newSingleThreadScheduledExecutor(), period, unit);
    }
    
    public PortMonitor(ScheduledExecutorService scheduler, long period, TimeUnit unit)
    {
        this.scheduler = scheduler;
        this.period = period;
        this.unit = unit;
        ports.addAll(SerialChannel.getAllPorts());
    }
    
    public void stop()
    {
        newPortConsumers.clear();
        newFreePortConsumers.clear();
        removePortConsumers.clear();
    }
    public void addNewPortConsumer(Consumer<String> portConsumer)
    {
        this.newPortConsumers.add(portConsumer);
        ensureRunning();
    }

    public void addNewFreePortConsumer(Consumer<String> portConsumer)
    {
        this.newFreePortConsumers.add(portConsumer);
        ensureRunning();
    }

    public void addRemoveFreePortConsumer(Consumer<String> portConsumer)
    {
        this.removeFreePortConsumers.add(portConsumer);
        ensureRunning();
    }

    public void addRemovePortConsumer(Consumer<String> portConsumer)
    {
        this.removePortConsumers.add(portConsumer);
        ensureRunning();
    }

    public void removeNewPortConsumer(Consumer<String> portConsumer)
    {
        this.newPortConsumers.remove(portConsumer);
        ensureRunning();
    }

    public void removeNewFreePortConsumer(Consumer<String> portConsumer)
    {
        this.newFreePortConsumers.remove(portConsumer);
        ensureRunning();
    }

    public void removeFreeRemovePortConsumer(Consumer<String> portConsumer)
    {
        this.removeFreePortConsumers.remove(portConsumer);
        ensureRunning();
    }

    public void removeRemovePortConsumer(Consumer<String> portConsumer)
    {
        this.removePortConsumers.remove(portConsumer);
        ensureRunning();
    }

    private void ensureRunning()
    {
        if (newPortConsumers.isEmpty() && newFreePortConsumers.isEmpty() && removePortConsumers.isEmpty())
        {
            if (future != null && !future.isDone())
            {
                future.cancel(false);
                future = null;
            }
        }
        else
        {
            if (future == null || future.isDone())
            {
                future = scheduler.scheduleAtFixedRate(this::checkPorts, 0, period, unit);
            }
        }
    }
    
    private void checkPorts()
    {
        checkPorts(ports, new HashSet<>(SerialChannel.getAllPorts()), newPortConsumers, removePortConsumers);
        checkPorts(freePorts, new HashSet<>(SerialChannel.getFreePorts()), newFreePortConsumers, removeFreePortConsumers);
    }
    private void checkPorts(Set<String> last, Set<String> now, List<Consumer<String>> newCons, List<Consumer<String>> removeCons)
    {
        if (!last.equals(now))
        {
            Set<String> newPorts = Sets.difference(now, last);
            Set<String> removedPorts = Sets.difference(last, now);
            newPorts.forEach((p)->newCons.forEach((c)->c.accept(p)));
            removedPorts.forEach((p)->removeCons.forEach((c)->c.accept(p)));
            last.removeAll(removedPorts);
            last.addAll(newPorts);
        }
    }
}
