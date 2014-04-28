package com.vannux.wemeet.entities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Event {
	private long id = 0;
	private Date creationdate;
    private Date eventdate;
    private String isPublic;
    private String name;
    private String description;
    private String city;
    private String location;
    private double geolat;
    private double geolon;
    private long country;
    private long createdby;
    private List<Long>users;
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public Date getCreationdate() {
		return creationdate;
	}
	public void setCreationdate(Date creationdate) {
		this.creationdate = creationdate;
	}
	public Date getEventdate() {
		return eventdate;
	}
	public void setEventdate(Date eventdate) {
		this.eventdate = eventdate;
	}
	public String getIsPublic() {
		return isPublic;
	}
	public void setIsPublic(String isPublic) {
		this.isPublic = isPublic;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public double getGeolat() {
		return geolat;
	}
	public void setGeolat(double geolat) {
		this.geolat = geolat;
	}
	public double getGeolon() {
		return geolon;
	}
	public void setGeolon(double geolon) {
		this.geolon = geolon;
	}
	public long getCountry() {
		return country;
	}
	public void setCountry(long country) {
		this.country = country;
	}
	public long getCreatedby() {
		return createdby;
	}
	public void setCreatedby(long createdby) {
		this.createdby = createdby;
	}
	public List<Long> getUsers() {
		return users;
	}
	public void setUsers(List<Long> users) {
		this.users = users;
	}
	@Override
	public String toString() {
		StringBuffer humanReadable = new StringBuffer(this.name);
		if (this.eventdate != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			humanReadable.append(" (" + sdf.format(this.eventdate) + ")");
		}
		return humanReadable.toString();
	}
}
