/*
 * Author:  Boris Guenther
 * Date:    27.12.2018
 * Time:    16:56:25
 */
package borg.ed.cz.data;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * TargetedShip
 *
 * @author <a href="mailto:boris.guenther@redteclab.com">Boris Guenther</a>
 */
@Getter
@Setter
public class TargetedShip {
    
    private String ship = null;
    
    private String pilotName = null;
    
    private String pilotRank = null;
    
    private BigDecimal bounty = null;
    
    private long lastSeen = System.currentTimeMillis();

    @Override
    public String toString() {
        return "TargetedShip [ship=" + ship + ", pilotName=" + pilotName + ", pilotRank=" + pilotRank + ", bounty=" + bounty + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TargetedShip other = (TargetedShip) obj;
        return this.toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

}
