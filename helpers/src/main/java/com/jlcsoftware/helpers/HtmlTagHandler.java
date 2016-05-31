/**
 * HtmlTagHandler.java
 * Copyright 2014 by NoticeWare Corporation
 *
 * @author Jeff
 *         Created: Jul 15, 2014
 */

package com.jlcsoftware.helpers;

import android.graphics.Color;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StrikethroughSpan;

import org.xml.sax.XMLReader;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * HtmlTagHandler
 *
 * @author Jeff
 *         from: http://stackoverflow.com/questions/4044509/android-how-to-use-the-html-taghandler
 *         To handle more text options
 *         
 *         
 *         Android supports: 
 *         
    <a href="...">
    <b>
    <big>
    <blockquote>
    <br>
    <cite>
    <dfn>
    <div align="...">
    <em>
    <font size="..." color="..." face="...">
    <h1>
    <h2>
    <h3>
    <h4>
    <h5>
    <h6>
    <i>
    <img src="...">
    <p>
    <small>
    <strike>
    <strong>
    <sub>
    <sup>
    <tt>
    <u>

 *         
 *         
 */
public class HtmlTagHandler implements Html.TagHandler {

    private List<Object> _format_stack = new LinkedList<Object>();
    private Vector<String> listParents = new Vector<String>();

    @Override
    public void handleTag(boolean open_tag, String tag, Editable output, XMLReader r) {

        if (tag.equals("ul") || tag.equals("ol")) {
            listItemCount = 0;
            if (open_tag) {
                listParents.add(tag);
                output.append("\n");
            } else
                listParents.remove(tag);
        } else if (tag.startsWith("li") && !open_tag) {
            handleListTag(output);
        }/* NOT TESTED... else if (tag.matches(".[a-fA-F0-9]{6}")) {
            processBackgroundColor(open_tag, output, tag.substring(1));
         } else if (tag.equalsIgnoreCase("strike") || tag.equals("s")) {
            processStrike(open_tag, output);
         } */
    }

    private int listItemCount = 0;

    private void handleListTag(Editable output) {
        if (listParents.lastElement().equals("ul")) {
            output.append("\n");
            String[] split = output.toString().split("\n");

            int lastIndex = split.length - 1;
            int start = output.length() - split[lastIndex].length() - 1;
            output.setSpan(new BulletSpan(15 * listParents.size()), start, output.length(), 0);
        } else if (listParents.lastElement().equals("ol")) {
            listItemCount++;

            output.append("\n");
            String[] split = output.toString().split("\n");

            int lastIndex = split.length - 1;
            int start = output.length() - split[lastIndex].length() - 1;
            output.insert(start, listItemCount + ". ");
            output.setSpan(new LeadingMarginSpan.Standard(15 * listParents.size()), start, output.length(), 0);
        }
    }

    private void processBackgroundColor(boolean open_tag, Editable output, String color) {
        final int length = output.length();
        if (open_tag) {
            final Object format = new BackgroundColorSpan(Color.parseColor('#' + color));
            _format_stack.add(format);
            output.setSpan(format, length, length, Spanned.SPAN_MARK_MARK);
        } else {
            applySpan(output, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void processStrike(boolean opening, Editable output) {
        int len = output.length();
        if (opening) {
            output.setSpan(new StrikethroughSpan(), len, len, Spannable.SPAN_MARK_MARK);
        } else {
            Object obj = getLast(output, StrikethroughSpan.class);
            int where = output.getSpanStart(obj);

            output.removeSpan(obj);

            if (where != len) {
                output.setSpan(new StrikethroughSpan(), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private Object getLast(Editable text, Class kind) {
        @SuppressWarnings("unchecked")
        final Object[] spans = text.getSpans(0, text.length(), kind);

        if (spans.length != 0)
            for (int i = spans.length; i > 0; i--)
                if (text.getSpanFlags(spans[i - 1]) == Spannable.SPAN_MARK_MARK)
                    return spans[i - 1];

        return null;
    }

    private void applySpan(Editable output, int length, int flags) {
        if (_format_stack.isEmpty())
            return;

        final Object format = _format_stack.remove(0);
        final Object span = getLast(output, format.getClass());
        final int where = output.getSpanStart(span);

        output.removeSpan(span);

        if (where != length)
            output.setSpan(format, where, length, flags);
    }
    
    
    /* exists solely to remove the extra '\n' inserted for <p></p> by Html.fromHtml(source);
     * 
     */

    public static Spanned fromHtml(String source) {
        Spanned text = Html.fromHtml(source, null, new HtmlTagHandler());
        return singlizeNewLines(text);
    }

    /* exists solely to remove the extra '\n' inserted for <p></p> by Html.fromHtml(source);
     * 
     */
    public static Spanned fromHtml(String source, Html.ImageGetter imageGetter, Html.TagHandler tagHandler) {
        Spanned text = Html.fromHtml(source, imageGetter, tagHandler);
        return singlizeNewLines(text);
    }

    public static Spanned singlizeNewLines(Spanned text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);

        for (int i = builder.length() - 1; i > 0; i--) {
            if (builder.charAt(i) == '\n') {
                if (builder.charAt(i - 1) == '\n') {
                    builder = builder.delete(i, i + 1);
                }
            }
        }
        return builder;
    }

}
