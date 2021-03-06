/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

package jogamp.newt;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.event.NEWTEventConsumer;

import jogamp.newt.event.NEWTEventTask;
import com.jogamp.newt.util.EDTUtil;
import java.util.ArrayList;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;

public abstract class DisplayImpl extends Display {
    private static int serialno = 1;

    private static Class<?> getDisplayClass(String type) 
        throws ClassNotFoundException 
    {
        final Class<?> displayClass = NewtFactory.getCustomClass(type, "DisplayDriver");
        if(null==displayClass) {
            throw new ClassNotFoundException("Failed to find NEWT Display Class <"+type+".DisplayDriver>");            
        }
        return displayClass;
    }

    /** Make sure to reuse a Display with the same name */
    public static Display create(String type, String name, final long handle, boolean reuse) {
        try {
            Class<?> displayClass = getDisplayClass(type);
            DisplayImpl display = (DisplayImpl) displayClass.newInstance();
            name = display.validateDisplayName(name, handle);
            synchronized(displayList) {
                if(reuse) {
                    Display display0 = Display.getLastDisplayOf(type, name, -1, true /* shared only */);
                    if(null != display0) {
                        if(DEBUG) {
                            System.err.println("Display.create() REUSE: "+display0+" "+getThreadName());
                        }
                        return display0;
                    }
                }
                display.exclusive = !reuse;
                display.name = name;
                display.type=type;
                display.refCount=0;
                display.id = serialno++;
                display.fqname = getFQName(display.type, display.name, display.id);
                display.hashCode = display.fqname.hashCode();
                displayList.add(display);
            }
            if(null == display.edtUtil) {
                display.setEDTUtil(null); // device's default if EDT is used, or null        
            }
            if(DEBUG) {
                System.err.println("Display.create() NEW: "+display+" "+getThreadName());
            }
            return display;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DisplayImpl other = (DisplayImpl) obj;
        if (this.id != other.id) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public  synchronized final void createNative()
        throws NativeWindowException
    {
        if(null==aDevice) {
            if(DEBUG) {
                System.err.println("Display.createNative() START ("+getThreadName()+", "+this+")");
            }
            final DisplayImpl f_dpy = this;
            try {
                runOnEDTIfAvail(true, new Runnable() {
                    public void run() {
                        f_dpy.createNativeImpl();
                    }});
            } catch (Throwable t) {
                throw new NativeWindowException(t);
            }
            if(null==aDevice) {
                throw new NativeWindowException("Display.createNative() failed to instanciate an AbstractGraphicsDevice");
            }
            if(DEBUG) {
                System.err.println("Display.createNative() END ("+getThreadName()+", "+this+")");
            }
            synchronized(displayList) {
                displaysActive++;
            }
        }
    }

    protected EDTUtil createEDTUtil() {
        final EDTUtil def;
        if(NewtFactory.useEDT()) {
            def = new DefaultEDTUtil(Thread.currentThread().getThreadGroup(), "Display-"+getFQName(), dispatchMessagesRunnable);            
            if(DEBUG) {
                System.err.println("Display.createNative("+getFQName()+") Create EDTUtil: "+def.getClass().getName());
            }
        } else {
            def = null;
        }
        return def;
    }

    @Override
    public EDTUtil setEDTUtil(EDTUtil newEDTUtil) {        
        if(null == newEDTUtil) {
            newEDTUtil = createEDTUtil();
        } else if( newEDTUtil == edtUtil ) {
            if(DEBUG) {
                System.err.println("Display.setEDTUtil: "+newEDTUtil+" - keep!");
            }
            return null; // no change
        }
        final EDTUtil oldEDTUtil = edtUtil;
        if(DEBUG) {
            System.err.println("Display.setEDTUtil: "+oldEDTUtil+" -> "+newEDTUtil);
        }
        removeEDT( new Runnable() { public void run() {} } );
        edtUtil = newEDTUtil;
        return oldEDTUtil;
    }

    @Override
    public final EDTUtil getEDTUtil() {
        return edtUtil;
    }

    private void removeEDT(final Runnable task) {
        if(null!=edtUtil) {            
            edtUtil.invokeStop(task);
            // ready for restart ..
            edtUtil.waitUntilStopped();
            edtUtil.reset();
        } else {
            task.run();
        }
    }

    public void runOnEDTIfAvail(boolean wait, final Runnable task) {
        if( null!=edtUtil && !edtUtil.isCurrentThreadEDT()) {
            edtUtil.invoke(wait, task);
        } else {
            task.run();
        }
    }

    public boolean validateEDT() {
        if(0==refCount && null==aDevice && null != edtUtil && edtUtil.isRunning()) {
            removeEDT( new Runnable() {
                public void run() {
                    // nop
                }
            } );
            return true;
        }
        return false;
    }

    @Override
    public synchronized final void destroy() {
        if(DEBUG) {
            dumpDisplayList("Display.destroy("+getFQName()+") BEGIN");
        }
        synchronized(displayList) {
            displayList.remove(this);
            if(0 < displaysActive) {
                displaysActive--;
            }
        }
        if(DEBUG) {
            System.err.println("Display.destroy(): "+this+" "+getThreadName());
        }
        final AbstractGraphicsDevice f_aDevice = aDevice;
        final DisplayImpl f_dpy = this;
        removeEDT( new Runnable() {
            public void run() {
                if ( null != f_aDevice ) {
                    f_dpy.closeNativeImpl();
                }
            }
        } );
        aDevice = null;
        refCount=0;
        if(DEBUG) {
            dumpDisplayList("Display.destroy("+getFQName()+") END");
        }
    }

    public synchronized final int addReference() {
        if(DEBUG) {
            System.err.println("Display.addReference() ("+DisplayImpl.getThreadName()+"): "+refCount+" -> "+(refCount+1));
        }
        if ( 0 == refCount ) {
            createNative();
        }
        if(null == aDevice) {
            throw new NativeWindowException ("Display.addReference() (refCount "+refCount+") null AbstractGraphicsDevice");
        }
        return refCount++;
    }


    public synchronized final int removeReference() {
        if(DEBUG) {
            System.err.println("Display.removeReference() ("+DisplayImpl.getThreadName()+"): "+refCount+" -> "+(refCount-1));
        }
        refCount--; // could become < 0, in case of manual destruction without actual creation/addReference
        if(0>=refCount) {
            destroy();
            refCount=0; // fix < 0
        }
        return refCount;
    }

    public synchronized final int getReferenceCount() {
        return refCount;
    }

    protected abstract void createNativeImpl();
    protected abstract void closeNativeImpl();

    @Override
    public final int getId() {
        return id;
    }

    @Override
    public final String getType() {
        return type;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String getFQName() {
        return fqname;
    }
    
    @Override
    public final boolean isExclusive() {
        return exclusive;
    }

    public static final String nilString = "nil" ;

    public String validateDisplayName(String name, long handle) {
        if(null==name && 0!=handle) {
            name="wrapping-"+toHexString(handle);
        }
        return ( null == name ) ? nilString : name ;
    }

    private static String getFQName(String type, String name, int id) {
        if(null==type) type=nilString;
        if(null==name) name=nilString;
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append("_");
        sb.append(name);
        sb.append("-");
        sb.append(id);
        return sb.toString();
    }

    @Override
    public final long getHandle() {
        if(null!=aDevice) {
            return aDevice.getHandle();
        }
        return 0;
    }

    @Override
    public final AbstractGraphicsDevice getGraphicsDevice() {
        return aDevice;
    }

    @Override
    public synchronized final boolean isNativeValid() {
        return null != aDevice;
    }

    @Override
    public boolean isEDTRunning() {
        if(null!=edtUtil) {
            return edtUtil.isRunning();
        }
        return false;
    }

    @Override
    public String toString() {
        return "NEWT-Display["+getFQName()+", excl "+exclusive+", refCount "+refCount+", hasEDT "+(null!=edtUtil)+", edtRunning "+isEDTRunning()+", "+aDevice+"]";
    }

    /** Dispatch native Toolkit messageges */
    protected abstract void dispatchMessagesNative();

    private Object eventsLock = new Object();
    private ArrayList<NEWTEventTask> events = new ArrayList<NEWTEventTask>();
    private volatile boolean haveEvents = false;

    class DispatchMessagesRunnable implements Runnable {
        public void run() {
            DisplayImpl.this.dispatchMessages();
        }
    }
    protected DispatchMessagesRunnable dispatchMessagesRunnable = new DispatchMessagesRunnable();

    final void dispatchMessage(final NEWTEventTask eventTask) {
        final NEWTEvent event = eventTask.get();
        try { 
            if(null == event) {
                // Ooops ?
                System.err.println("Warning: event of eventTask is NULL");
                Thread.dumpStack();
                return;
            }
            final Object source = event.getSource();        
            if(source instanceof NEWTEventConsumer) {
                final NEWTEventConsumer consumer = (NEWTEventConsumer) source ;
                if(!consumer.consumeEvent(event)) {
                    // enqueue for later execution
                    enqueueEvent(false, event);
                }
            } else {
                throw new RuntimeException("Event source not NEWT: "+source.getClass().getName()+", "+source);
            }
        } catch (Throwable t) {
            final RuntimeException re;
            if(t instanceof RuntimeException) {
                re = (RuntimeException) t;
            } else {
                re = new RuntimeException(t);
            }
            if( eventTask.isCallerWaiting() ) {
                // propagate exception to caller
                eventTask.setException(re);
            } else {
                throw re;
            }
        }
        eventTask.notifyCaller();        
    }
    
    @Override
    public void dispatchMessages() {
        // System.err.println("Display.dispatchMessages() 0 "+this+" "+getThreadName());
        if(0==refCount || // no screens 
           null==getGraphicsDevice() // no native device
          ) 
        {
            return;
        }

        ArrayList<NEWTEventTask> _events = null;

        if(haveEvents) { // volatile: ok
            synchronized(eventsLock) {
                if(haveEvents) {
                    // swap events list to free ASAP
                    _events = events;
                    events = new ArrayList<NEWTEventTask>();
                    haveEvents = false;
                }
                eventsLock.notifyAll();
            }
            if( null != _events ) {
                for (int i=0; i < _events.size(); i++) {
                    dispatchMessage(_events.get(i));
                }
            }
        }

        // System.err.println("Display.dispatchMessages() NATIVE "+this+" "+getThreadName());
        dispatchMessagesNative();
    }

    public void enqueueEvent(boolean wait, NEWTEvent e) {
        if(!isEDTRunning()) {
            // oops .. we are already dead
            if(DEBUG) {
                Throwable t = new Throwable("Warning: EDT already stopped: wait:="+wait+", "+e);
                t.printStackTrace();
            }
            return;
        }
        
        // can't wait if we are on EDT or NEDT -> consume right away
        if(wait && edtUtil.isCurrentThreadEDTorNEDT() ) {
            dispatchMessage(new NEWTEventTask(e, null));
            return;
        }
        
        final Object lock = new Object();
        final NEWTEventTask eTask = new NEWTEventTask(e, wait?lock:null);
        synchronized(lock) {
            synchronized(eventsLock) {
                events.add(eTask);
                haveEvents = true;
                eventsLock.notifyAll();
            }
            if( wait ) {
                try {
                    lock.wait();
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
                if( null != eTask.getException() ) {
                    throw eTask.getException();
                }
            }            
        }
    }

    public interface DisplayRunnable<T> {
        T run(long dpy);
    }    
    public static final <T> T runWithLockedDevice(AbstractGraphicsDevice device, DisplayRunnable<T> action) {
        T res;
        device.lock();
        try {
            res = action.run(device.getHandle());
        } finally {
            device.unlock();
        }
        return res;
    }
    public final <T> T runWithLockedDisplayDevice(DisplayRunnable<T> action) {
        final AbstractGraphicsDevice device = getGraphicsDevice();
        if(null == device) {
            throw new RuntimeException("null device - not initialized: "+this);
        }
        return runWithLockedDevice(device, action);
    }
    
    protected EDTUtil edtUtil = null;
    protected int id;
    protected String name;
    protected String type;
    protected String fqname;
    protected int hashCode;
    protected int refCount; // number of Display references by Screen
    protected boolean exclusive; // do not share this display, uses NullLock!
    protected AbstractGraphicsDevice aDevice;
}

