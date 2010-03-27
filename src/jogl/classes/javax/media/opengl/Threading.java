/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package javax.media.opengl;

import com.jogamp.opengl.impl.*;

/** This API provides access to the threading model for the implementation of 
    the classes in this package.

    <P>

    OpenGL is specified as a thread-safe API, but in practice there
    are multithreading-related issues on most, if not all, of the
    platforms which support it. For example, some OpenGL
    implementations do not behave well when one context is made
    current first on one thread, released, and then made current on a
    second thread, although this is legal according to the OpenGL
    specification. On other platforms there are other problems.

    <P>

    Due to these limitations, and due to the inherent multithreading
    in the Java platform (in particular, in the Abstract Window
    Toolkit), it is often necessary to limit the multithreading 
    occurring in the typical application using the OpenGL API. 

    <P>

    In the current reference implementation, for instance, multithreading 
    has been limited by
    forcing all OpenGL-related work for GLAutoDrawables on to a single
    thread. In other words, if an application uses only the
    GLAutoDrawable and GLEventListener callback mechanism, it is
    guaranteed to have the most correct single-threaded behavior on
    all platforms.

    <P>

    Applications using the GLContext makeCurrent/release API directly
    will inherently break this single-threaded model, as these methods
    require that the OpenGL context be made current on the current
    thread immediately. For applications wishing to integrate better
    with an implementation that uses the single-threaded model, this
    class provides public access to the mechanism used by the implementation.

    <P>

    Users can execute Runnables on the
    internal thread used for performing OpenGL work, and query whether
    the current thread is already this thread. Using these mechanisms
    the user can move work from the current thread on to the internal
    OpenGL thread if desired.

    <P>

    This class also provides mechanisms for querying whether this
    internal serialization of OpenGL work is in effect, and a
    programmatic way of disabling it.  In the current reference 
    implementation it is enabled by default, although it could be 
    disabled in the future if OpenGL drivers become more robust on 
    all platforms.

    <P>

    In addition to specifying programmatically whether the single
    thread for OpenGL work is enabled, users may switch it on and off
    using the system property <code>opengl.1thread</code>. Valid values
    for this system property are:

    <PRE>
    -Dopengl.1thread=false     Disable single-threading of OpenGL work
    -Dopengl.1thread=true      Enable single-threading of OpenGL work (default -- on a newly-created worker thread)
    -Dopengl.1thread=auto      Select default single-threading behavior (currently on)
    -Dopengl.1thread=awt       Enable single-threading of OpenGL work on AWT event dispatch thread (current default on all
                                 platforms, and also the default behavior older releases)
    -Dopengl.1thread=worker    Enable single-threading of OpenGL work on newly-created worker thread (not suitable for Mac
                                 OS X or X11 platforms, and risky on Windows in applet environments)
    </PRE>    
*/

public class Threading {

    /** No reason to ever instantiate this class */
    private Threading() {}

    /** If an implementation of the javax.media.opengl APIs offers a 
        multithreading option but the default behavior is single-threading, 
        this API provides a mechanism for end users to disable single-threading 
        in this implementation.  Users are strongly discouraged from
        calling this method unless they are aware of all of the
        consequences and are prepared to enforce some amount of
        threading restrictions in their applications. Disabling
        single-threading, for example, may have unintended consequences
        on GLAutoDrawable implementations such as GLCanvas, GLJPanel and
        GLPbuffer. Currently there is no supported way to re-enable it
        once disabled, partly to discourage careless use of this
        method. This method should be called as early as possible in an
        application. */ 
    public static void disableSingleThreading() {
        ThreadingImpl.disableSingleThreading();
    }

    /** Indicates whether OpenGL work is being automatically forced to a
        single thread in this implementation. */
    public static boolean isSingleThreaded() {
        return ThreadingImpl.isSingleThreaded();
    }

    /** Indicates whether the current thread is the single thread on
        which this implementation of the javax.media.opengl APIs
        performs all of its OpenGL-related work. This method should only
        be called if the single-thread model is in effect. */
    public static boolean isOpenGLThread() throws GLException {
        return ThreadingImpl.isOpenGLThread();
    }

    /** Executes the passed Runnable on the single thread used for all
        OpenGL work in this javax.media.opengl API implementation. It is
        not specified exactly which thread is used for this
        purpose. This method should only be called if the single-thread
        model is in use and if the current thread is not the OpenGL
        thread (i.e., if <code>isOpenGLThread()</code> returns
        false). It is up to the end user to check to see whether the
        current thread is the OpenGL thread and either execute the
        Runnable directly or perform the work inside it. */
    public static void invokeOnOpenGLThread(Runnable r) throws GLException {
        ThreadingImpl.invokeOnOpenGLThread(r);
    }
}
