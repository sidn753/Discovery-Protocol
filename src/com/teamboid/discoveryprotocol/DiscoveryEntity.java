package com.teamboid.discoveryprotocol;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.JSONObject;

import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

/**
 * Represents another device using the discovery protocol.
 * <br/><br/>
 * This class is Parcelable, so it can be passed as an extra in an intent.
 * @author Aidan Follestad
 */
public class DiscoveryEntity implements Parcelable {

	public DiscoveryEntity(InetAddress ip, JSONObject json) {
		_address = ip;
		_id = json.optString("id");
		_name = json.optString("name");
		_status = json.optString("status");
	}

	private InetAddress _address;
	private String _id;
	private String _name;
	private String _status;
	
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
	public InetAddress getAddress() {
		return _address;
	}
	
	/**
	 * Convenience method. Returns the value of {@link #getName()}, unless it's null, 
	 * in which case it returns the value of {@link #getAddress()}.getHostAddress(). 
	 */
	public String getDisplay() {
		if(getName() == null || getName().trim().isEmpty()) {
			return getAddress().getHostAddress();
		} else { 
			return getName();
		}
	}
	
	/**
	 * Gets a unique identifier for the entity that no other entity will have (this value
	 * is derived from the serial number of an Android device).
	 * @return
	 */
	public String getID() {
		return _id;
	}
	
	/**
	 * Gets the status message of the entity.
	 */
	public String getStatus() {
		return _status;
	}

	@Override
	public String toString() {
		return "DiscoveryRequest [" + _name + ", " + _address.getHostAddress() + ", " + _status + "]";
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(_address.getHostAddress());
		dest.writeString(_id);
		dest.writeString(_name);
		dest.writeString(_status);
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
		try {
			_address = InetAddress.getByName(in.readString());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		_id = in.readString();
		_name = in.readString();
		_status = in.readString();
	}
}
