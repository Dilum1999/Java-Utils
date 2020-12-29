package com.ibm.courts.img;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class TrimImage {
private BufferedImage img;
	
	public TrimImage(File input) {
        try {
            img = ImageIO.read(input);
        } catch (IOException e) {
            throw new RuntimeException( "Problem reading image", e );
        }
    }
	public void trim() {
		WritableRaster raster = this.img.getAlphaRaster();
	    int width = raster.getWidth();
	    int height = raster.getHeight();
	    int left = 0;
	    int top = 0;
	    int right = width - 1;
	    int bottom = height - 1;
	    int minRight = width - 1;
	    int minBottom = height - 1;
	    top:
	        for (;top < bottom; top++){
	            for (int x = 0; x < width; x++){
	                if (raster.getSample(x, top, 0) != 0){
	                    minRight = x;
	                    minBottom = top;
	                    break top;
	                }
	            }
	        }
	    left:
	        for (;left < minRight; left++){
	            for (int y = height - 1; y > top; y--){
	                if (raster.getSample(left, y, 0) != 0){
	                    minBottom = y;
	                    break left;
	                }
	            }
	        }
	    bottom:
	        for (;bottom > minBottom; bottom--){
	            for (int x = width - 1; x >= left; x--){
	                if (raster.getSample(x, bottom, 0) != 0){
	                    minRight = x;
	                    break bottom;
	                }
	            }
	        }
	    right:
	        for (;right > minRight; right--){
	            for (int y = bottom; y >= top; y--){
	                if (raster.getSample(right, y, 0) != 0){
	                    break right;
	                }
	            }
	        }

	    img = img.getSubimage(left, top, right - left + 1, bottom - top + 1);
	}
	
	public void write(File f) {
        try {
            ImageIO.write(img, "png", f);
        } catch (IOException e) {
            throw new RuntimeException( "Problem writing image", e );
        }
    }
	public static void main(String[] args) {
		TrimImage trim = new TrimImage(new File("not-trimmed.png"));// TODO Add a transparent png image, can be found in this repo or my react demo
        trim.trim();
        trim.write(new File("trimmedImg.png"));
    }
}
