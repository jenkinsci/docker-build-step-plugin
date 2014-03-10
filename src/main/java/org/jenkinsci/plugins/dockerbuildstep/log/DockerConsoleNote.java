package org.jenkinsci.plugins.dockerbuildstep.log;

import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotationDescriptor;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;
import hudson.model.Run;

/**
 * Annotator which adds color highlighting. There are currently two message categories: error, staring with <i>ERROR:</i>
 * prefix, and info, which starts with <i>INFO:</i> prefix.
 * 
 * @author vjuranek
 * 
 */
public class DockerConsoleNote extends ConsoleNote<Run<?, ?>> {

    @Override
    public ConsoleAnnotator<Run<?, ?>> annotate(Run<?, ?> context, MarkupText text, int charPos) {
        if (text.getText().contains("ERROR:"))
            text.addMarkup(0, text.length(), "<span style=\"font-weight: bold; color:red\">", "</span>");
        if (text.getText().contains("WARN:"))
            text.addMarkup(0, text.length(), "<span style=\"color:#FF8700\">", "</span>");
        if (text.getText().contains("INFO:"))
            text.addMarkup(0, text.length(), "<span style=\"color:#008BB8\">", "</span>");
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends ConsoleAnnotationDescriptor {
        public String getDisplayName() {
            return "Docker Console Note";
        }
    }

    private static final long serialVersionUID = 1L;
}