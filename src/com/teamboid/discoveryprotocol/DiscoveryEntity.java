package com.teamboid.discoveryprotocol;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents another device using the discovery protocol.
 * 
 * @author Aidan Follestad
 */
public class DiscoveryEntity implements Parcelable {

	public DiscoveryEntity(String name, InetAddress ip) {
		_name = name;
		_ip = ip;
	}

	private String _name;
	private InetAddress _ip;

	/**
	 * Gets the display name of the entity.
	 * 
	 * @return
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Gets the IP address of the entity.
	 * 
	 * @return
	 */
	public InetAddress getIP() {
		return _ip;
	}

	@Override
	public String toString() {
		return "DiscoveryRequest [" + _name + ", " + _ip.getHostAddress() + "]";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getName());
		dest.writeString(getIP().getHostAddress());
	}

	public static final Parcelable.Creator<DiscoveryEntity> CREATOR = new Parcelable.Creator<DiscoveryEntity>() {
		public DiscoveryEntity createFromParcel(Parcel in) {
			return new DiscoveryEntity(in);
		}

		public DiscoveryEntity[] newArray(int size) {
			return new DiscoveryEntity[size];
		}
	};

	private DiscoveryEntity(Parcel in) {
		_name = in.readString();
		try {
			_ip = InetAddress.getByName(in.readString());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
