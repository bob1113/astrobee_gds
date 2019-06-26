/*******************************************************************************
 * Copyright (c) 2013 United States Government as represented by the 
 * Administrator of the National Aeronautics and Space Administration. 
 * All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package gov.nasa.arc.verve.common.ardor3d.text;

import gov.nasa.util.URIUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.URLResourceSource;

/**
 * Loads a font generated by BMFont ({@link http://www.angelcode.com/products/bmfont/}).
 * <ul>
 * <li>Font info file *must* be in XML format.
 * <li>The texture should be saved in 32 bit PNG format - TGA does not appear to work.
 * <li>This class only supports a single page (see BMFont documentation for details on pages)
 * </ul>
 */
public class BMFont {
    private static Logger logger = Logger.getLogger(BMFont.class.getName());

    private final HashMap<Integer, Char> _charMap = new HashMap<Integer, Char>();
    private final HashMap<Integer, HashMap<Integer, Integer>> _kernMap = new HashMap<Integer, HashMap<Integer, Integer>>();

    private String _styleName; // e.g. "Courier-12-bold"
    private final ArrayList<Page> _pages = new ArrayList<Page>();
    private Texture _pageTexture;
    private RenderStateSetter _blendStateSetter = null;
    private RenderStateSetter _alphaStateSetter = null;
    private final boolean _useMipMaps;
    private int _maxCharAdv;
    private Common _common = null;
    private Info _info = null;
    private float _defaultLodBias = -0.6f;
    private float _defaultAnisotropy = 1;

    /**
     * Reads an XML BMFont description file and loads corresponding texture. Note that the TGA written by BMFont does
     * not seem to be read properly by the Ardor3D loader. PNG works fine.
     * 
     * @param fileUrl
     * @param useMipMaps
     *            if true, use trilinear filtering with max anisotropy, else min filter is bilinear. MipMaps result in
     *            blurrier text, but less shimmering.
     * @throws URISyntaxException
     * @throws IOException
     */
    public BMFont(final URL fileUrl, final boolean useMipMaps) throws URISyntaxException, IOException {
        _useMipMaps = useMipMaps;
        final File fontFile = new File(URIUtil.toURI(fileUrl));
        parseFontFile(fontFile);
        initialize(fontFile);
    }
    
    /**
     * Reads an XML BMFont description file and loads corresponding texture. Note that the TGA written by BMFont does
     * not seem to be read properly by the Ardor3D loader. PNG works fine.
     * 
     * @param source ResourceSource describing where this font comes from.
     * @param useMipMaps
     *            if true, use trilinear filtering with max anisotropy, else min filter is bilinear. MipMaps result in
     *            blurrier text, but less shimmering.
     * @throws URISyntaxException
     * @throws IOException
     */
    public BMFont(final ResourceSource source, final boolean useMipMaps) throws IOException {
        _useMipMaps = useMipMaps;
        parseFontFile(source);
        initialize(source);
    }

    public void setBlendStateSetter(RenderStateSetter setter) {
        _blendStateSetter = setter;
    }
    
    public void setAlphaStateSetter(RenderStateSetter setter) {
        _alphaStateSetter = setter;
    }
    
    public Texture getPageTexture() {
        return _pageTexture;
    }
    
    /** apply default render states to spatial */
    public void applyRenderStatesTo(final Spatial spatial, final boolean useBlend) {
        if (useBlend) {
            if (_blendStateSetter == null) {
                _blendStateSetter = new RenderStateSetter(_pageTexture, true);
            }
            _blendStateSetter.applyTo(spatial);
        } else {
            if (_alphaStateSetter == null) {
                _alphaStateSetter = new RenderStateSetter(_pageTexture, false);
            }
            _alphaStateSetter.applyTo(spatial);
        }
    }

    public String getStyleName() {
        return _styleName;
    }

    public int getSize() {
        return _info.size;
    }

    /**
     * The average of _common.base and _common.lineHeight seems to be the best general purpose value
     */
    public int getLineHeight() {
        return (_common.base + _common.lineHeight) / 2;
    }

    public int getTextureWidth() {
        return _common.scaleW;
    }

    public int getTextureHeight() {
        return _common.scaleH;
    }

    /**
     * @param chr
     *            ascii character code
     * @return character descriptor for chr. If character is not in the char set, return '?' (if '?' is not in the char
     *         set, return will be null)
     */
    public BMFont.Char getChar(int chr) {
        BMFont.Char retVal = _charMap.get(chr);
        if (retVal == null) {
            chr = '?';
            retVal = _charMap.get(chr);
            if (retVal == null) { // if still null, use the first char
                final Iterator<Char> it = _charMap.values().iterator();
                retVal = it.next();
            }
        }
        return retVal;
    }

    /**
     * @return kerning information for this character pair
     */
    public int getKerning(final int chr, final int nextChr) {
        final HashMap<Integer, Integer> map = _kernMap.get(chr);
        if (map != null) {
            final Integer amt = map.get(nextChr);
            if (amt != null) {
                return amt;
            }
        }
        return 0;
    }

    /**
     * @return the largest xadvance in this char set
     */
    public int getMaxCharAdvance() {
        return _maxCharAdv;
    }

    public int getOutlineWidth() {
        return _info.outline;
    }

    /**
     * load the texture and create default render states. Only a single page is supported.
     */
    // ----------------------------------------------------------
    protected void initialize(final File fontFile) throws MalformedURLException {
        _styleName = _info.face + "-" + _info.size;

        if (_info.bold) {
            _styleName += "-bold";
        } else {
            _styleName += "-medium";
        }

        if (_info.italic) {
            _styleName += "-italic";
        } else {
            _styleName += "-regular";
        }

        // only a single page is supported
        final String parentPath = fontFile.getParent();
        if (_pages.size() > 0) {
            final Page page = _pages.get(0);
            final File texFile = new File(parentPath + File.separator + page.file);
            final URI texUri = texFile.toURI();
            final URL texUrl = texUri.toURL();
            
            Texture.MinificationFilter minFilter;
            Texture.MagnificationFilter magFilter;

            magFilter = Texture.MagnificationFilter.Bilinear;
            minFilter = Texture.MinificationFilter.BilinearNoMipMaps;
            if (_useMipMaps) {
                minFilter = Texture.MinificationFilter.Trilinear;
            }
            final TextureKey tkey = TextureKey.getKey(new URLResourceSource(texUrl), false, TextureStoreFormat.GuessNoCompressedFormat, minFilter);
            _pageTexture = TextureManager.loadFromKey(tkey, null, null);
            _pageTexture.setMagnificationFilter(magFilter);

            if (_useMipMaps) {
                _pageTexture.setAnisotropicFilterPercent(_defaultAnisotropy);
                _pageTexture.setLodBias(_defaultLodBias);
                //System.out.println("font lod bias = "+_defaultLodBias);
            }
        }
    }
    
    /**
     * load the texture and create default render states. Only a single page is supported.
     */
    // ----------------------------------------------------------
    @SuppressWarnings("unused")
    protected void initialize(final ResourceSource source) throws MalformedURLException {
        _styleName = _info.face + "-" + _info.size;

        if (_info.bold) {
            _styleName += "-bold";
        } else {
            _styleName += "-medium";
        }

        if (_info.italic) {
            _styleName += "-italic";
        } else {
            _styleName += "-regular";
        }

        // only a single page is supported
        if (_pages.size() > 0) {
            final Page page = _pages.get(0);
            final ResourceSource texSrc = source.getRelativeSource(page.file);

            Texture.MinificationFilter minFilter;
            Texture.MagnificationFilter magFilter;

            magFilter = Texture.MagnificationFilter.Bilinear;
            minFilter = Texture.MinificationFilter.BilinearNoMipMaps;
            if (_useMipMaps) {
                minFilter = Texture.MinificationFilter.Trilinear;
            }
            final TextureKey tkey = TextureKey.getKey(texSrc, false, TextureStoreFormat.GuessNoCompressedFormat, minFilter);
            _pageTexture = TextureManager.loadFromKey(tkey, null, null);
            _pageTexture.setMagnificationFilter(magFilter);

            if (_useMipMaps) {
                _pageTexture.setAnisotropicFilterPercent(_defaultAnisotropy);
                _pageTexture.setLodBias(_defaultLodBias);
                //System.out.println("font lod bias = "+_defaultLodBias);
            }
        }
    }

    /**
     * 
     * @param fileUrl
     * @throws IOException
     */
    protected void parseFontFile(final File fontFile) throws IOException {
        _maxCharAdv = 0;
        _charMap.clear();
        _pages.clear();
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.parse(fontFile);

            doc.getDocumentElement().normalize();
            recurse(doc.getFirstChild());

            db.reset();
        } catch (final Throwable t) {
            final IOException ex = new IOException("Error loading font file " + fontFile.getPath());
            ex.initCause(t);
            throw ex;
        }
    }

    /**
     * 
     * @param fileUrl
     * @throws IOException
     */
    protected void parseFontFile(final ResourceSource source) throws IOException {
        _maxCharAdv = 0;
        _charMap.clear();
        _pages.clear();
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.parse(source.openStream());

            doc.getDocumentElement().normalize();
            recurse(doc.getFirstChild());

            db.reset();
        } catch (final Throwable t) {
            final IOException ex = new IOException("Error loading font file " + source.toString());
            ex.initCause(t);
            throw ex;
        }
    }

    
    private void recurse(final Node node) {
        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            processNode(child);
            recurse(child);
        }
    }

    private void processNode(final Node node) {
        final String tagName = node.getNodeName();
        if (tagName != null) {
            if (tagName.equals("info")) {
                processInfoNode(node);
            } else if (tagName.equals("common")) {
                processCommonNode(node);
            } else if (tagName.equals("page")) {
                processPageNode(node);
            } else if (tagName.equals("char")) {
                processCharNode(node);
            } else if (tagName.equals("kerning")) {
                procesKerningNode(node);
            }
        }
    }

    private void processInfoNode(final Node node) {
        final NamedNodeMap attribs = node.getAttributes();
        _info = new Info();
        _info.face = getStringAttrib("face", attribs);
        _info.size = getIntAttrib("size", attribs);
        _info.bold = getBoolAttrib("bold", attribs);
        _info.italic = getBoolAttrib("italic", attribs);
        _info.charset = getStringAttrib("charset", attribs);
        _info.unicode = getBoolAttrib("unicode", attribs);
        _info.stretchH = getIntAttrib("stretchH", attribs);
        _info.smooth = getBoolAttrib("smooth", attribs);
        _info.aa = getBoolAttrib("aa", attribs);
        _info.padding = getIntArrayAttrib("padding", attribs);
        _info.spacing = getIntArrayAttrib("spacing", attribs);
        _info.outline = getIntAttrib("outline", attribs);
    }

    private void processCommonNode(final Node node) {
        final NamedNodeMap attribs = node.getAttributes();
        _common = new Common();
        _common.lineHeight = getIntAttrib("lineHeight", attribs);
        _common.base = getIntAttrib("base", attribs);
        _common.scaleW = getIntAttrib("scaleW", attribs);
        _common.scaleH = getIntAttrib("scaleH", attribs);
        _common.pages = getIntAttrib("pages", attribs);
        _common.packed = getBoolAttrib("packed", attribs);
        _common.alphaChnl = getIntAttrib("alphaChnl", attribs);
        _common.redChnl = getIntAttrib("redChnl", attribs);
        _common.greenChnl = getIntAttrib("greenChnl", attribs);
        _common.blueChnl = getIntAttrib("blueChnl", attribs);
    }

    private void processCharNode(final Node node) {
        final NamedNodeMap attribs = node.getAttributes();
        final Char c = new Char();
        c.id = getIntAttrib("id", attribs);
        c.x = getIntAttrib("x", attribs);
        c.y = getIntAttrib("y", attribs);
        c.width = getIntAttrib("width", attribs);
        c.height = getIntAttrib("height", attribs);
        c.xoffset = getIntAttrib("xoffset", attribs);
        c.yoffset = getIntAttrib("yoffset", attribs);
        c.xadvance = getIntAttrib("xadvance", attribs);
        c.page = getIntAttrib("page", attribs);
        c.chnl = getIntAttrib("chnl", attribs);
        _charMap.put(c.id, c);
        if (c.xadvance > _maxCharAdv) {
            _maxCharAdv = c.xadvance;
        }
    }

    private void processPageNode(final Node node) {
        final NamedNodeMap attribs = node.getAttributes();
        final Page page = new Page();
        page.id = getIntAttrib("id", attribs);
        page.file = getStringAttrib("file", attribs);
        _pages.add(page);
        if (_pages.size() > 1) {
            logger.warning("multiple pages defined in font description file, but only a single page is supported.");
        }
    }

    private void procesKerningNode(final Node node) {
        final NamedNodeMap attribs = node.getAttributes();
        final int first = getIntAttrib("first", attribs);
        final int second = getIntAttrib("second", attribs);
        final int amount = getIntAttrib("amount", attribs);
        HashMap<Integer, Integer> amtHash;
        amtHash = _kernMap.get(first);
        if (amtHash == null) {
            amtHash = new HashMap<Integer, Integer>();
            _kernMap.put(first, amtHash);
        }
        amtHash.put(second, amount);
    }

    // == xml attribute getters ============================
    int getIntAttrib(final String name, final NamedNodeMap attribs) {
        final Node node = attribs.getNamedItem(name);
        return Integer.parseInt(node.getNodeValue());
    }

    String getStringAttrib(final String name, final NamedNodeMap attribs) {
        final Node node = attribs.getNamedItem(name);
        return node.getNodeValue();
    }

    boolean getBoolAttrib(final String name, final NamedNodeMap attribs) {
        final Node node = attribs.getNamedItem(name);
        return (Integer.parseInt(node.getNodeValue()) == 1);
    }

    int[] getIntArrayAttrib(final String name, final NamedNodeMap attribs) {
        final Node node = attribs.getNamedItem(name);
        final String str = node.getNodeValue();
        final StringTokenizer strtok = new StringTokenizer(str, ",");
        final int sz = strtok.countTokens();
        final int[] retVal = new int[sz];
        for (int i = 0; i < sz; i++) {
            retVal[i] = Integer.parseInt(strtok.nextToken());
        }
        return retVal;
    }

    // == support structs ==================================
    public class Info {
        public String face;
        public int size;
        public boolean bold;
        public boolean italic;
        public String charset;
        public boolean unicode;
        public int stretchH;
        public boolean smooth;
        public boolean aa;
        public int[] padding;
        public int[] spacing;
        public int outline;
    }

    public class Common {
        public int lineHeight;
        public int base;
        public int scaleW;
        public int scaleH;
        public int pages;
        public boolean packed;
        public int alphaChnl;
        public int redChnl;
        public int greenChnl;
        public int blueChnl;
    }

    public class Page {
        public int id;
        public String file;
    }

    public class Char {
        public int id;
        public int x;
        public int y;
        public int width;
        public int height;
        public int xoffset;
        public int yoffset;
        public int xadvance;
        public int page;
        public int chnl;
    }

    /**
     * utility to set default render states for text
     */
    public class RenderStateSetter {
        public TextureState textureState;
        public BlendState blendState;
        public ZBufferState zBuffState;

        float _blendDisabledTestRef = 0.23f;
        float _blendEnabledTestRef = 0.02f;

        boolean _useBlend;

        public RenderStateSetter(final Texture texture, final boolean useBlend) {
            textureState = new TextureState();
            textureState.setTexture(texture);

            blendState = new BlendState();
            blendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
            blendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
            blendState.setTestEnabled(true);
            blendState.setTestFunction(BlendState.TestFunction.GreaterThan);

            zBuffState = new ZBufferState();
            zBuffState.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);

            setUseBlend(useBlend);
        }

        public void applyTo(final Spatial spatial) {
            spatial.setRenderState(textureState);
            spatial.setRenderState(blendState);
            spatial.setRenderState(zBuffState);
            if (_useBlend) {
                //spatial.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
                spatial.getSceneHints().setRenderBucketType(RenderBucketType.PostBucket);
            } else {
                spatial.getSceneHints().setRenderBucketType(RenderBucketType.Opaque);
            }
        }

        public void setUseBlend(final boolean blend) {
            _useBlend = blend;
            if (blend == false) {
                blendState.setBlendEnabled(false);
                blendState.setReference(_blendDisabledTestRef);
                zBuffState.setWritable(true);
            } else {
                blendState.setBlendEnabled(true);
                blendState.setReference(_blendEnabledTestRef);
                zBuffState.setWritable(false);
            }
        }
    }
}