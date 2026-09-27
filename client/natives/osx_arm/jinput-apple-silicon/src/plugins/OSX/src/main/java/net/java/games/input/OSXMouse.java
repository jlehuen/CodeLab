/*
 * %W% %E%
 *
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
/*****************************************************************************
* Copyright (c) 2003 Sun Microsystems, Inc.  All Rights Reserved.
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* - Redistribution of source code must retain the above copyright notice,
*   this list of conditions and the following disclaimer.
*
* - Redistribution in binary form must reproduce the above copyright notice,
*   this list of conditions and the following disclaimer in the documentation
*   and/or other materails provided with the distribution.
*
* Neither the name Sun Microsystems, Inc. or the names of the contributors
* may be used to endorse or promote products derived from this software
* without specific prior written permission.
*
* This software is provided "AS IS," without a warranty of any kind.
* ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
* ANY IMPLIED WARRANT OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
* NON-INFRINGEMEN, ARE HEREBY EXCLUDED.  SUN MICROSYSTEMS, INC. ("SUN") AND
* ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS
* A RESULT OF USING, MODIFYING OR DESTRIBUTING THIS SOFTWARE OR ITS
* DERIVATIVES.  IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
* REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
* INCIDENTAL OR PUNITIVE DAMAGES.  HOWEVER CAUSED AND REGARDLESS OF THE THEORY
* OF LIABILITY, ARISING OUT OF THE USE OF OUR INABILITY TO USE THIS SOFTWARE,
* EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
*
* You acknowledge that this software is not designed or intended for us in
* the design, construction, operation or maintenance of any nuclear facility
*
*****************************************************************************/
package net.java.games.input;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

/** Represents an OSX Mouse
* @author elias
* @version 1.0
*/
final class OSXMouse extends Mouse {
	private final PortType port;
	private final OSXHIDQueue queue;
	private final boolean isInternalTrackpad;
	private final Queue<Event> pendingEvents = new ArrayDeque<>();
	private final float[] pollData = new float[5];
	private float lastLeft = 0.0f;
	private float lastRight = 0.0f;
	private float lastMiddle = 0.0f;

	protected OSXMouse(OSXHIDDevice device, OSXHIDQueue queue, Component[] components, Controller[] children, Rumbler[] rumblers) {
		super(device.getProductName(), components, children, rumblers);
		this.queue = queue;
		this.port = device.getPortType();
		String name = device.getProductName();
		this.isInternalTrackpad = (name != null && name.toLowerCase().contains("internal"));
	}

	protected final void pollDevice() throws IOException {
		if (!isInternalTrackpad) {
			return;
		}
		nPollPointer(pollData);
		float dx = pollData[0];
		float dy = pollData[1];
		float left = pollData[2];
		float right = pollData[3];
		float middle = pollData[4];
		long now = System.nanoTime();

		if (dx != 0.0f && getX() != null) {
			Event ev = new Event();
			ev.set(getX(), dx, now);
			pendingEvents.add(ev);
		}
		if (dy != 0.0f && getY() != null) {
			Event ev = new Event();
			ev.set(getY(), dy, now);
			pendingEvents.add(ev);
		}
		if (left != lastLeft && getPrimaryButton() != null) {
			lastLeft = left;
			Event ev = new Event();
			ev.set(getPrimaryButton(), left, now);
			pendingEvents.add(ev);
		}
		if (right != lastRight && getSecondaryButton() != null) {
			lastRight = right;
			Event ev = new Event();
			ev.set(getSecondaryButton(), right, now);
			pendingEvents.add(ev);
		}
		if (middle != lastMiddle && getTertiaryButton() != null) {
			lastMiddle = middle;
			Event ev = new Event();
			ev.set(getTertiaryButton(), middle, now);
			pendingEvents.add(ev);
		}
	}

	protected final boolean getNextDeviceEvent(Event event) throws IOException {
		if (OSXControllers.getNextDeviceEvent(event, queue)) {
			return true;
		}
		if (!pendingEvents.isEmpty()) {
			Event next = pendingEvents.poll();
			event.set(next.getComponent(), next.getValue(), next.getNanos());
			return true;
		}
		return false;
	}

	protected final void setDeviceEventQueueSize(int size) throws IOException {
		queue.setQueueDepth(size);
		pendingEvents.clear();
	}

	public final PortType getPortType() {
		return port;
	}

	private static native void nPollPointer(float[] data);
}
