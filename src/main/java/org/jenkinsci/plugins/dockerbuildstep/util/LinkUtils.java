package org.jenkinsci.plugins.dockerbuildstep.util;

import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Links;

public class LinkUtils {

    public static final String ALIAS_SEPARATOR = ":";
    public static final String LINK_SEPARATOR = ",";
    
    public static Links parseLinks(String linksStr) throws IllegalArgumentException {
        if(linksStr == null || linksStr.equals(""))
            return new Links();
        
        String[] linksSplit = linksStr.split(LINK_SEPARATOR);
        Link[] links = new Link[linksSplit.length];
        for(int i = 0; i < linksSplit.length; i++) {
            links[i] = Link.parse(linksSplit[i]);
        }
        return new Links(links);
    }
    
    public static String asString(Links links) {
        if (links == null || links.getLinks() == null || links.getLinks().length == 0)
            return "";
        
        StringBuilder sb = new StringBuilder();
        for(Link link : links.getLinks()) {
            sb.append(link.getName()).append(ALIAS_SEPARATOR).append(link.getAlias()).append(LINK_SEPARATOR);
        }
        sb.deleteCharAt(sb.length() - 1); //remove trailing comma, size is always non-zero
        return sb.toString();
    }
    
}
