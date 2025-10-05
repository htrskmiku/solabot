package com.arth.bot.core.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName t_pjsk_binding
 */
@TableName(value ="t_pjsk_binding")
@Data
public class PjskBinding {
    /**
     * 
     */
    @TableId
    private Long id;

    /**
     * 
     */
    private String pjskId;

    /**
     * 
     */
    private Long qqNumber;

    /**
     * 
     */
    private Long groupId;

    /**
     * 
     */
    private String serverRegion;

    /**
     * 
     */
    private Date createdAt;

    /**
     * 
     */
    private Date updatedAt;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        PjskBinding other = (PjskBinding) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getPjskId() == null ? other.getPjskId() == null : this.getPjskId().equals(other.getPjskId()))
            && (this.getQqNumber() == null ? other.getQqNumber() == null : this.getQqNumber().equals(other.getQqNumber()))
            && (this.getGroupId() == null ? other.getGroupId() == null : this.getGroupId().equals(other.getGroupId()))
            && (this.getServerRegion() == null ? other.getServerRegion() == null : this.getServerRegion().equals(other.getServerRegion()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getPjskId() == null) ? 0 : getPjskId().hashCode());
        result = prime * result + ((getQqNumber() == null) ? 0 : getQqNumber().hashCode());
        result = prime * result + ((getGroupId() == null) ? 0 : getGroupId().hashCode());
        result = prime * result + ((getServerRegion() == null) ? 0 : getServerRegion().hashCode());
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
        result = prime * result + ((getUpdatedAt() == null) ? 0 : getUpdatedAt().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", pjskId=").append(pjskId);
        sb.append(", qqNumber=").append(qqNumber);
        sb.append(", groupId=").append(groupId);
        sb.append(", serverRegion=").append(serverRegion);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append("]");
        return sb.toString();
    }
}