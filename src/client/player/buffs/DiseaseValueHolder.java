/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.player.buffs;

import java.io.Serializable;

/**
 *
 * @author GabrielSin
 */
public class DiseaseValueHolder implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    public long startTime;
    public long length;
    public Disease disease;

    public DiseaseValueHolder(final Disease disease, final long startTime, final long length) {
        this.disease = disease;
        this.startTime = startTime;
        this.length = length;
    }
}

