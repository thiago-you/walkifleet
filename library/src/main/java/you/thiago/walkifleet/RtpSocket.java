/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package you.thiago.walkifleet;

import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;

/**
 * RtpSocket implements a RTP socket for receiving and sending RTP packets.
 * <p>
 * RtpSocket is associated to a DatagramSocket that is used to send and/or
 * receive RtpPackets.
 */
public class RtpSocket
{
	private static final String TAG = "RtpSocket";
	public static boolean verbose = false;

	/** UDP socket */
	DatagramSocket socket;

	/** Remote address */
	InetAddress r_addr;

	/** Remote port */
	int r_port;

	DatagramPacket datagram;
	
	/** Creates a new RTP socket (only receiver) */
	public RtpSocket(DatagramSocket datagram_socket)
	{
		try {
			socket = datagram_socket;
			r_addr = null;
			r_port = 0;
			datagram = new DatagramPacket(new byte[1], 1);
		}catch (Exception ex){}
	}

	/** Creates a new RTP socket (sender and receiver) */
	public RtpSocket(DatagramSocket datagram_socket, InetAddress remote_address, int remote_port)
	{
		try {
			socket = datagram_socket;
			r_addr = remote_address;
			r_port = remote_port;
			datagram = new DatagramPacket(new byte[1],1);
		}catch (Exception ex){}
	}

	/** Returns the RTP DatagramSocket */
	public DatagramSocket getDatagramSocket()
	{
		return socket;
	}

	/** Receives a RTP packet from this socket */
	public void receive(RtpPacket rtpp) throws IOException
	{
		datagram.setData(rtpp.packet);
		datagram.setLength(rtpp.packet.length);
		socket.receive(datagram);
		rtpp.packet_len = datagram.getLength();
	}

	/** Sends a RTP packet from this socket */
	public void send(RtpPacket rtpp) throws IOException
	{
    	if (verbose)
    	{
    		int seq = rtpp.getSequenceNumber();
    	}

    	byte[] data = new byte[rtpp.packet_len];
		System.arraycopy(rtpp.packet, 0, data, 0, rtpp.packet_len);
		DatagramPacket datagram = new DatagramPacket(data, data.length);
		datagram.setAddress(r_addr);
		datagram.setPort(r_port);
		socket.send(datagram);
	}

	/** Sends a RTP packet from this socket */
	public void send(byte[] rtpp) throws IOException
	{
		byte[] data = new byte[rtpp.length];
		System.arraycopy(rtpp, 0, data, 0, rtpp.length);
		DatagramPacket datagram = new DatagramPacket(data, data.length);
		datagram.setAddress(r_addr);
		datagram.setPort(r_port);
		socket.send(datagram);
	}

	/** Closes this socket */
	public void close()
	{
		socket.close();
	}
}
