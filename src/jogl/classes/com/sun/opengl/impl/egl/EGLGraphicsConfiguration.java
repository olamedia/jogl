/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.impl.egl;

import java.util.*;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.gluegen.runtime.NativeLibrary;

public class EGLGraphicsConfiguration extends DefaultGraphicsConfiguration implements Cloneable {
    
    public _EGLConfig getNativeConfig() {
        return _config;
    }

    public int getNativeConfigID() {
        return configID;
    }

    public EGLGraphicsConfiguration(AbstractGraphicsScreen screen, GLCapabilities caps, _EGLConfig cfg, int cfgID) {
        super(screen, caps);
        _config = cfg;
        configID = cfgID;
    }

    public Object clone() {
        return super.clone();
    }

    public static _EGLConfig EGLConfigId2EGLConfig(GLProfile glp, long display, int configID) {
        int[] attrs = new int[] {
                EGL.EGL_RENDERABLE_TYPE, -1,
                EGL.EGL_CONFIG_ID, configID,
                EGL.EGL_NONE
            };
        if (glp.usesNativeGLES2()) {
            attrs[1] = EGL.EGL_OPENGL_ES2_BIT;
        } else if (glp.usesNativeGLES1()) {
            attrs[1] = EGL.EGL_OPENGL_ES_BIT;
        } else {
            attrs[1] = EGL.EGL_OPENGL_BIT;
        }
        _EGLConfig[] configs = new _EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL.eglChooseConfig(display,
                                 attrs, 0,
                                 configs, 1,
                                 numConfigs, 0)) {
            return null;
        }
        if (numConfigs[0] == 0) {
            return null;
        }
        return configs[0];
    }

    public static GLCapabilities EGLConfig2Capabilities(GLProfile glp, long display, _EGLConfig _config) {
        GLCapabilities caps = new GLCapabilities(glp);
        int[] val = new int[1];

        // Read the actual configuration into the choosen caps
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_RED_SIZE, val, 0)) {
            caps.setRedBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_GREEN_SIZE, val, 0)) {
            caps.setGreenBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_BLUE_SIZE, val, 0)) {
            caps.setBlueBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_ALPHA_SIZE, val, 0)) {
            caps.setAlphaBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_STENCIL_SIZE, val, 0)) {
            caps.setStencilBits(val[0]);
        }
        if(EGL.eglGetConfigAttrib(display, _config, EGL.EGL_DEPTH_SIZE, val, 0)) {
            caps.setDepthBits(val[0]);
        }
        return caps;
    }

    public static int[] GLCapabilities2AttribList(GLCapabilities caps) {
        int[] attrs = new int[] {
                EGL.EGL_RENDERABLE_TYPE, -1,
                // FIXME: does this need to be configurable?
                EGL.EGL_SURFACE_TYPE,    EGL.EGL_WINDOW_BIT,
                EGL.EGL_RED_SIZE,        caps.getRedBits(),
                EGL.EGL_GREEN_SIZE,      caps.getGreenBits(),
                EGL.EGL_BLUE_SIZE,       caps.getBlueBits(),
                EGL.EGL_ALPHA_SIZE,      (caps.getAlphaBits() > 0 ? caps.getAlphaBits() : EGL.EGL_DONT_CARE),
                EGL.EGL_STENCIL_SIZE,    (caps.getStencilBits() > 0 ? caps.getStencilBits() : EGL.EGL_DONT_CARE),
                EGL.EGL_DEPTH_SIZE,      caps.getDepthBits(),
                EGL.EGL_NONE
            };

        if(caps.getGLProfile().usesNativeGLES1()) {
            attrs[1] = EGL.EGL_OPENGL_ES_BIT;
        }
        else if(caps.getGLProfile().usesNativeGLES2()) {
            attrs[1] = EGL.EGL_OPENGL_ES2_BIT;
        } else {
            attrs[1] = EGL.EGL_OPENGL_BIT;
        }

        return attrs;
    }

    public String toString() {
        return getClass().toString()+"["+getScreen()+", eglConfigID "+configID+ ", "+getCapabilities()+"]";
    }
    private _EGLConfig _config;
    private int configID;
}

