package com.ablesky.asdeploy.pojo;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="patch_group")
public class PatchGroup extends AbstractModel {

	@Id
	@GeneratedValue
	@Column
	private Long id;
	@ManyToOne
	@JoinColumn(name="creator_id")
	private User creator;
	@ManyToOne
	@JoinColumn(name="project_id")
	private Project project;
	@Column
	private String name;
	@Column(name="check_code")
	private String checkCode;
	@Column
	private String status;
	@Column(name="create_time")
	private Timestamp createTime;
	@Column(name="finish_time")
	private Timestamp finishTime;
	
	public PatchGroup() {}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public User getCreator() {
		return creator;
	}
	public void setCreator(User creator) {
		this.creator = creator;
	}
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCheckCode() {
		return checkCode;
	}
	public void setCheckCode(String checkCode) {
		this.checkCode = checkCode;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Timestamp getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}
	public Timestamp getFinishTime() {
		return finishTime;
	}
	public void setFinishTime(Timestamp finishTime) {
		this.finishTime = finishTime;
	}
	
}
