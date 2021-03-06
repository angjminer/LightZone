/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.utils.ColorMatrix2;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.IntVibranceOpImage;
import com.lightcrafts.jai.opimage.HueRotateOpImage;

import com.lightcrafts.mediax.jai.*;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: Mar 15, 2005
 * Time: 3:33:17 PM
 */
public class HueSaturationOperation extends BlendedOperation {
    static final String HUE = "Hue";
    static final String SATURATION = "Saturation";
    static final String VIBRANCE = "Vibrance";
    static final String LUMINOSITY = "Luminosity";
    //angelo contrast
    static final String CONTRAST = "Contrast";

    public HueSaturationOperation(Rendering rendering, OperationType type) {
        super(rendering, type);
        colorInputOnly = true;

        if (type != typeV2)
            addSliderKey(HUE);
        addSliderKey(SATURATION);
        if (type != typeV1)
            addSliderKey(VIBRANCE);
        if (type != typeV2)
            addSliderKey(LUMINOSITY);
        if (type == typeV2)
            addSliderKey(HUE);
        //angelo contrast
        addSliderKey(CONTRAST);

        DecimalFormat format = new DecimalFormat("0");

        setSliderConfig(HUE, new SliderConfig(-180, 180, hue, 1, false, format));
        setSliderConfig(SATURATION, new SliderConfig(-100, 100, saturation, 1, false, format));
        //angelo contrast
        setSliderConfig(CONTRAST, new SliderConfig(-127, 127, contr, 1, false, format));
        if (type != typeV1)
            setSliderConfig(VIBRANCE, new SliderConfig(-100, 100, vibrance, 1, false, format));
        if (type != typeV2)
            setSliderConfig(LUMINOSITY, new SliderConfig(-100, 100, intensity, 1, false, format));
    }

    public boolean neutralDefault() {
        return true;
    }

    static final OperationType typeV1 = new OperationTypeImpl("Hue/Saturation");
    static final OperationType typeV2 = new OperationTypeImpl("Hue/Saturation V2");
    static final OperationType typeV3 = new OperationTypeImpl("Hue/Saturation V3");

    private float hue = 0;
    private float saturation = 0;
    private float vibrance = 0;
    private float intensity = 0;
    //angelo contrast
    private float contr = 0;

    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);
        
        if (key == HUE && hue != value) {
            hue = (float) value;
        } else if (key == SATURATION && saturation != value) {
            saturation = (float) value;
        } else if (key == VIBRANCE && vibrance != value) {
            vibrance = (float) value;
        } else if (key == LUMINOSITY && intensity != value) {
            intensity = (float) value;
        //angelo contrast    
        } else if (key == CONTRAST && contr != value) {
            contr = (float) value;
        } else
            return;
        
        super.setSliderValue(key, value);
    }

    private double[][] computeTransform() {
        float matrix[][] = {
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1},
        };

        if (saturation != 0.0)
            ColorMatrix2.saturatemat(matrix, saturation / 100 + 1);

//        if (hue != 0.0)
//            ColorMatrix2.huerotatemat(matrix, hue);

        if (intensity != 0.0) {
            float lit = intensity / 100 + 1;
            ColorMatrix2.cscalemat(matrix, lit, lit, lit);
        }
        //angelo contrast
        if (contr != 0.0) {

            ColorMatrix2.contrastmat(matrix, contr, intensity);
        }

        double transform[][] = new double[3][4];

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 4; j++)
                transform[i][j] = matrix[j][i];

        return transform;
    }

    private float[][] computeVibranceTransform() {
        float matrix[][] = {
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1},
        };

        if (vibrance != 0.0)
            ColorMatrix2.saturatemat(matrix, vibrance / 100 + 1);

        float transform[][] = new float[3][4];

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 4; j++)
                transform[i][j] = matrix[j][i];

        return transform;
    }

    private class HueSaturation extends BlendedTransform {
        HueSaturation(PlanarImage source) {
            super(source);
        }

        public PlanarImage setFront() {
            double hslTransform[][] = computeTransform();
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(back);
            pb.add(hslTransform);
            PlanarImage image = JAI.create("BandCombine", pb, JAIContext.noCacheHint);

            if (vibrance != 0.0)
                image = new IntVibranceOpImage(image, computeVibranceTransform(), null);

            if (hue != 0.0)
                image = new HueRotateOpImage(image, (float) (hue / 360), null);

            return image;
        }
    }

    protected void updateOp(Transform op) {
        op.update();
    }

    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new HueSaturation(source);
    }

    public OperationType getType() {
        return type;
    }
}
