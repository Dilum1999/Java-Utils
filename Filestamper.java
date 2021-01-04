package com.ibm.courts.img;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;

import org.w3c.dom.*;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFEncodeParam;



public class Filestamper implements Serializable {

	private static final long serialVersionUID = -2549579248311948013L;
	static final String CRICUIT_CLERK_OWNER = "CIRCUIT CLERK";
	static final String SOFTWARE_CATEGORY_TYPE = "SOFTWARE";
	static final String NO_FILESTAMP_IF_TICKET_CATEGORY = "NO_FILESTAMP_IF_TICKET";
	static final String NO_FILESTAMP_CATEGORY = "NO_FILESTAMP";
	static final String SW_CASE_TYPE = "SW";
	static final String AO_CASE_TYPE = "AO";
	static final String NON_CASE_CATEGORY = "NONCASE";
	static final String CRIMINAL_CATEGORY = "CRIMINAL";

	byte[] imageContents;
	List<PlanarImage> tiffPages;
	String sourceFileName;
	Date timestamp;
    boolean biLevelImage = true;
   
    public static Date autoFileStampCSWgrpsStartDate, autoFilestampNonCSStartDate;
    public static String CS_WKGRP_CODE = "CS";
    public static String JACKET_ACT = "60500";
    public static byte[] CLERK_SIGNATURE;
   
    public enum StampDirective{
	    FileStamp,
	    DoNotFileStamp,
	    FileStampIfLetterSize
    }

    public Filestamper(String fileName){
	    sourceFileName = fileName;
	    initialize();
    }
   
    public Filestamper(byte[] tiffContents){
	    imageContents = tiffContents;
	    initialize();
    }
   
    public Filestamper(List<PlanarImage> pages){
	    tiffPages = pages;
	    initialize();
    }
    
   
    protected static void initialize(){
	    try{
		    if (CLERK_SIGNATURE == null)
		    CLERK_SIGNATURE = getFile("sign.png");//TODO Load a signature file here;
	    }catch(Exception ex){
	   
	    };
    }
   
    public boolean isLetterSizePage() {	
    	try {
    		loadImagePages();

			if (!tiffPages.isEmpty()) {
				PlanarImage planarImage = tiffPages.get(0);
				return letterSizePage(planarImage);
			}
		} catch (Exception ex) {
			
			}
    	return false;

    }

   
    public byte[] applyFilestampLocal(Date date)throws Exception{
	    timestamp = date;
	    List<BufferedImage> pages = processImage();
	    return writeIntoATiff(pages);
    }
   
	public List<BufferedImage> processImage() throws Exception {
	
		loadImagePages();
		int i = 0;
		boolean firstPage = true;
		BufferedImage page;
		List<BufferedImage> outPages = new ArrayList<BufferedImage>();
		for (Iterator iterator = tiffPages.iterator(); iterator.hasNext();) {
			PlanarImage planarImage = (PlanarImage) iterator.next();
			page = planarImage.getAsBufferedImage();
			if (page.getType() != BufferedImage.TYPE_BYTE_BINARY)
				biLevelImage = false;
			if (firstPage) {
				page = applyFilestamp(page);
				if (biLevelImage)
				page = convertToGrayScale(page);
				firstPage = false;
			}
			i++;
			outPages.add(page);
		}
		return outPages;
	}
	
	private boolean letterSizePage(PlanarImage page){
		int w = page.getWidth();
		int h = page.getHeight();
		if (page.getWidth() > page.getHeight()){
			h = page.getWidth();
			w = page.getHeight();
		}
		float ratio = ((float)h/(float)w);
		return (ratio > 1.22 && ratio < 1.35);
	}
	   
	public  void loadImagePages()
	throws Exception {
		if (tiffPages != null)
			return ;
	
		SeekableStream s ;
		FileInputStream inputStream = null;
		if (sourceFileName != null && new File(sourceFileName).exists()){
			inputStream = new FileInputStream(sourceFileName);
			s = SeekableStream.wrapInputStream(inputStream, true);
		}else{
			s = SeekableStream.wrapInputStream(new ByteArrayInputStream(imageContents), true);
		}
		
		ParameterBlock pb = new ParameterBlock();
		pb.add(s);
		ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, null);
		int count = dec.getNumPages();
		tiffPages = new ArrayList<PlanarImage>();
		for (int i = 0; i < count; i++) {
			RenderedImage page = dec.decodeAsRenderedImage(i);
			tiffPages.add(PlanarImage.wrapRenderedImage(page));
		}
		if (inputStream != null){
			inputStream.close();
		}
	}

	private  BufferedImage applyFilestamp(BufferedImage image) throws IOException {
	   
	    BufferedImage img = image;
	    if (biLevelImage)
	    img = convert(img);
	    Graphics2D g2d = img.createGraphics();
	    int width = img.getWidth();
	    int height = img.getHeight();
	    int[][] array = new int[width][height];
	    BufferedImage imgBnW;
	    if (biLevelImage)
	    imgBnW = image;
	    else
	    imgBnW = convertToGrayScale(img);
	    for(int i = width * 2/3; i < width; i++){
	    	for(int j = 0; j < height/3 ; j++){
	    		array[i][j] = imgBnW.getRGB(i, j);
	        }
	    }
   
	    BufferedImage fileStamp600x600 = createFilestamp();
        ////////////////////// If statement below will fix the issue
		float scaleSize = width/2600f;
		if(scaleSize > 1)
			scaleSize = 1;
        BufferedImage fileStamp = scale(fileStamp600x600, scaleSize);
        int fileStampWidth = fileStamp.getWidth()*width/2600;
        int fileStampHeight = fileStamp.getHeight()*width/2600;
	    int count = 0;
	    int tmpCount = 0;
	    int[] cordinates = new int[2];
	    for(int x = width * 2/3; x < width - fileStampWidth;) {
		    for(int y = 0; y < height/3 - fileStampHeight;) {
			    count = getWhitePixelCount(array,x,y,fileStampWidth,fileStampHeight);
			    if(count > tmpCount) {
				    tmpCount = count;
				    cordinates[0] = x;
				    cordinates[1] = y;
			    }
		    	y = y + 10;
		    }
		    x = x + 10;
	    }
	   
	    int x = cordinates[0];
	    int y = cordinates[1];
	   
       
        g2d.drawImage(fileStamp, x, y, null);
        g2d.dispose();
        return img;
	}

   
    public  byte[] writeIntoATiff(List<BufferedImage> pages)throws Exception{
		TIFFEncodeParam params = new TIFFEncodeParam();
		if (biLevelImage)
			params.setCompression(TIFFEncodeParam.COMPRESSION_GROUP4);//TODO
		else
			params.setCompression(TIFFEncodeParam.COMPRESSION_DEFLATE);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageEncoder encoder = ImageCodec.createImageEncoder("tiff", out, params);
        Iterator pagesIt = pages.iterator();
        pagesIt.next();
        params.setExtraImages(pagesIt);
        encoder.encode(pages.get(0));
        out.close();
	    return out.toByteArray();
    }
   

   
    public  BufferedImage convert(BufferedImage image) {
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel colorModel = new ComponentColorModel(
            colorSpace, false, false, Transparency.OPAQUE,
            DataBuffer.TYPE_USHORT);

        BufferedImageOp converter = new ColorConvertOp(colorSpace, null);
        BufferedImage newImage =
            converter.createCompatibleDestImage(image, colorModel);
        converter.filter(image, newImage);
        return newImage;
    }
   
    public  BufferedImage scale(BufferedImage before, float scale){
		int w = before.getWidth();
		int h = before.getHeight();
		BufferedImage after = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		AffineTransform at = new AffineTransform();
		at.scale(scale, scale);
		AffineTransformOp scaleOp =
		  new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		after = scaleOp.filter(before, after);
		return after;
    }
   
	private  BufferedImage convertToGrayScale(BufferedImage image) {
		final BufferedImage grayImage = new BufferedImage(image.getWidth(null),
		image.getHeight(null), BufferedImage.TYPE_BYTE_BINARY);
		final Graphics2D g = (Graphics2D) grayImage.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return grayImage;
	}

    private  int getWhitePixelCount(int[][] array, int x, int y, int width, int height) {
	    int count = 0;
	    for(int i = x; i < x + width;i++){
		    for(int j = y; j < y + height; j++){
			    if(array[i][j]==-1){
			    	count++;
			    }
		    }
	    }
	    return count;
    }
   

   
   
    public  BufferedImage createFilestamp(){
    	int width = 600, height = 600;
    	BufferedImage bufferedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);

        // Create a graphics contents on the buffered image
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        // Draw graphics
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        // Draw graphics
        g2d.setComposite(AlphaComposite.Src);
        try{
        	g2d.drawImage(ImageIO.read(new ByteArrayInputStream(CLERK_SIGNATURE)), 25, 200, null);
        }catch(Exception ex){
        	ex.printStackTrace();
        }
        if (!biLevelImage)
        	g2d.setColor(new Color(0,0,.8f,0.5f));
        else
	        g2d.setColor(new Color(0,0,0));//TODO
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(5, 5, 590, 590);
        g2d.setFont( new Font("Arial Narrow", Font.BOLD, 120));
        //g2d.drawString("e-FILED", 100,150);
        g2d.drawString("*FILED*", 105,150);
        g2d.setFont( new Font("Arial Narrow", Font.BOLD, 60));
       
        g2d.drawString(new SimpleDateFormat("MMM dd, yyyy hh:mm a").format(timestamp).toUpperCase(), 40, 210);
       
        g2d.setFont( new Font("Arial Narrow", Font.BOLD, 50));
        g2d.drawString("CLERK OF THE", 130, 450);
        g2d.drawString("18TH JUDICIAL CIRCUIT", 60, 510);
        g2d.drawString("DUPAGE COUNTY, ILLINOIS", 20, 570);
        g2d.dispose();

        return bufferedImage;
    }
   

	public static void main(String[] args) throws Exception {
		String fileName = "sample.tiff";//TODO Change the file name to match a given TIFF file
		Filestamper stamper = new Filestamper(getFile(fileName));
		byte[] stamped = stamper.applyFilestampLocal(new Date(System.currentTimeMillis()));
		FileOutputStream out = new FileOutputStream(new File(fileName + "out" + ".tiff"));
		out.write(stamped);
		out.close();
		
	}

    public static byte[] getFile(String fileName) throws Exception
    {
        File file=new File(fileName);
        FileInputStream streamer = new FileInputStream(file);
        byte[] byteArray=new byte[streamer.available()];
        for(int j=0; j<byteArray.length; j++)
        {
        	byteArray[j]=(byte)streamer.read();
        }
        return byteArray;
    }

}
