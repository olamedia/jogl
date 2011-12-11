/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 
package com.jogamp.opengl.test.junit.jogl.acore;

import java.io.IOException;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

public class TestShutdownSharedNEWT extends UITestCase {

    static long duration = 300; // ms
    
    protected void runTestGL() throws InterruptedException {
        GLWindow glWindow = GLWindow.create(new GLCapabilities(GLProfile.getDefault()));
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Gears NEWT Test");

        glWindow.addGLEventListener(new Gears());

        Animator animator = new Animator(glWindow);

        glWindow.setSize(256, 256);
        glWindow.setVisible(true);
        animator.setUpdateFPSFrames(60, System.err);
        animator.start();
        Assert.assertEquals(true, animator.isAnimating());
        Assert.assertEquals(true, glWindow.isVisible());
        Assert.assertEquals(true, glWindow.isNativeValid());
        Assert.assertEquals(true, glWindow.isRealized());

        while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        glWindow.destroy();
    }

    protected void oneLife() throws InterruptedException {
        GLProfile.initSingleton();
        runTestGL();        
        GLProfile.shutdown(GLProfile.ShutdownType.SHARED_ONLY);        
    }
    
    @Test
    public void test01OneLife() throws InterruptedException {
        oneLife();
    }

    @Test
    public void test01AnotherLife() throws InterruptedException {
        oneLife();
    }
    
    @Test
    public void test01TwoLifes() throws InterruptedException {
        oneLife();
        oneLife();
    }
    
    public static void main(String args[]) throws IOException {
        String tstname = TestShutdownSharedNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
