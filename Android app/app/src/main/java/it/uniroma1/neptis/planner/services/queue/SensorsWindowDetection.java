/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.services.queue;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.List;

public class SensorsWindowDetection {

    private List<Double> x;
    private List<Double> y;
    private List<Double> z;

    private FastFourierTransformer fft;

    private DescriptiveStatistics window;

    private int windowSize = 256;
    private int halfWindow = windowSize / 2;

    public SensorsWindowDetection() {
        this.fft = new FastFourierTransformer(DftNormalization.STANDARD);
    }

    public void setX(List<Double> x) {
        this.x = x;
    }

    public void setY(List<Double> y) {
        this.y = y;
    }

    public void setZ(List<Double> z) {
        this.z = z;
    }

    public double[] getFeatures() {
        double[] out = new double[9];
        Complex[] signal;
        double energy;
        //X
        double[] xArray = convertListToArray(x);
        signal = fft.transform(xArray, TransformType.FORWARD);
        energy = 0.0;
        for(Complex c : signal)
            energy += Math.pow(c.abs(),2);
        energy /= (double)windowSize;
        //Mean,StDev,Energy
        window = new DescriptiveStatistics(xArray);
        out[0] = window.getMean();
        out[3] = window.getStandardDeviation();
        out[6] = energy;

        //Y
        double[] yArray = convertListToArray(y);
        signal = fft.transform(yArray, TransformType.FORWARD);
        energy = 0.0;
        for(Complex c : signal)
            energy += Math.pow(c.abs(),2);
        energy /= (double)windowSize;
        //Mean,StDev,Energy
        window = new DescriptiveStatistics(yArray);
        out[1] = window.getMean();
        out[4] = window.getStandardDeviation();
        out[7] = energy;

        //Z
        double[] zArray = convertListToArray(z);
        signal = fft.transform(zArray, TransformType.FORWARD);
        energy = 0.0;
        for(Complex c : signal)
            energy += Math.pow(c.abs(),2);
        energy /= (double)windowSize;
        //Mean,StDev,Energy
        window = new DescriptiveStatistics(zArray);
        out[2] = window.getMean();
        out[5] = window.getStandardDeviation();
        out[8] = energy;

        return out;
    }

    private double[] convertListToArray(List<Double> list) {
        double[] out = new double[windowSize];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }
}
