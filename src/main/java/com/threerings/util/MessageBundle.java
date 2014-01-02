//
// ooo-util - a place for OOO utilities
// Copyright (C) 2011 Three Rings Design, Inc., All Rights Reserved
// http://github.com/threerings/ooo-util/blob/master/LICENSE

package com.threerings.util;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.samskivert.text.MessageUtil;
import com.samskivert.util.StringUtil;

import static com.threerings.util.Log.log;

/**
 * A message bundle provides an easy mechanism by which to obtain translated message strings from
 * a resource bundle. It uses the {@link MessageFormat} class to substitute arguments into the
 * translation strings. Message bundles would generally be obtained via the {@link MessageManager},
 * but could be constructed individually if so desired.
 */
public class MessageBundle
{
    /**
     * Call this to "taint" any string that has been entered by an entity outside the application
     * so that the translation code knows not to attempt to translate this string when doing
     * recursive translations (see {@link #xlate}).
     */
    public static String taint (Object text)
    {
        return MessageUtil.taint(text);
    }

    /**
     * Composes a message key with a single argument. The message can subsequently be translated
     * in a single call using {@link #xlate}.
     */
    public static String compose (String key, Object arg)
    {
        return MessageUtil.compose(key, new Object[] { arg });
    }

    /**
     * Composes a message key with an array of arguments. The message can subsequently be
     * translated in a single call using {@link #xlate}.
     */
    public static String compose (String key, Object... args)
    {
        return MessageUtil.compose(key, args);
    }

    /**
     * Composes a message key with an array of arguments. The message can subsequently be
     * translated in a single call using {@link #xlate}.
     */
    public static String compose (String key, String... args)
    {
        return MessageUtil.compose(key, args);
    }

    /**
     * A convenience method for calling {@link #compose(String,Object[])} with an array of
     * arguments that will be automatically tainted (see {@link #taint}).
     */
    public static String tcompose (String key, Object... args)
    {
        return MessageUtil.tcompose(key, args);
    }

    /**
     * Required for backwards compatibility. Alas.
     */
    public static String tcompose (String key, Object arg)
    {
        return MessageUtil.tcompose(key, new Object[] { arg });
    }

    /**
     * Required for backwards compatibility. Alas.
     */
    public static String tcompose (String key, Object arg1, Object arg2)
    {
        return MessageUtil.tcompose(key, new Object[] { arg1, arg2 });
    }

    /**
     * A convenience method for calling {@link #compose(String,String[])} with an array of
     * arguments that will be automatically tainted (see {@link #taint}).
     */
    public static String tcompose (String key, String... args)
    {
        return MessageUtil.tcompose(key, args);
    }

    /**
     * Returns a fully qualified message key which, when translated by some other bundle, will
     * know to resolve and utilize the supplied bundle to translate this particular key.
     */
    public static String qualify (String bundle, String key)
    {
        return MessageUtil.qualify(bundle, key);
    }

    /**
     * Returns the bundle name from a fully qualified message key.
     *
     * @see #qualify
     */
    public static String getBundle (String qualifiedKey)
    {
        return MessageUtil.getBundle(qualifiedKey);
    }

    /**
     * Returns the unqualified portion of the key from a fully qualified message key.
     *
     * @see #qualify
     */
    public static String getUnqualifiedKey (String qualifiedKey)
    {
        return MessageUtil.getUnqualifiedKey(qualifiedKey);
    }

    /**
     * Initializes the message bundle which will obtain localized messages from the supplied
     * resource bundle. The path is provided purely for reporting purposes.
     */
    public void init (MessageManager msgmgr, String path,
                      ResourceBundle bundle, MessageBundle parent)
    {
        _msgmgr = msgmgr;
        _path = path;
        _bundle = bundle;
        _parent = parent;
    }

    /**
     * Obtains the translation for the specified message key. No arguments are substituted into
     * the translated string. If a translation message does not exist for the specified key, an
     * error is logged and the key itself is returned so that the caller need not worry about
     * handling a null response.
     */
    public String get (String key)
    {
        // if this string is tainted, we don't translate it, instead we
        // simply remove the taint character and return it to the caller
        if (MessageUtil.isTainted(key)) {
            return MessageUtil.untaint(key);
        }

        String msg = getResourceString(key);
        return (msg != null) ? msg : key;
    }

    /**
     * Adds all messages whose key starts with the specified prefix to the supplied collection.
     *
     * @param includeParent if true, messages from our parent bundle (and its parent bundle, all
     * the way up the chain will be included).
     */
    public void getAll (String prefix, Collection<String> messages, boolean includeParent)
    {
        Enumeration<String> iter = _bundle.getKeys();
        while (iter.hasMoreElements()) {
            String key = iter.nextElement();
            if (key.startsWith(prefix)) {
                messages.add(get(key));
            }
        }
        if (includeParent && _parent != null) {
            _parent.getAll(prefix, messages, includeParent);
        }
    }

    /**
     * Adds all keys for messages whose key starts with the specified prefix to the supplied
     * collection.
     *
     * @param includeParent if true, messages from our parent bundle (and its parent bundle, all
     * the way up the chain will be included).
     */
    public void getAllKeys (String prefix, Collection<String> keys, boolean includeParent)
    {
        Enumeration<String> iter = _bundle.getKeys();
        while (iter.hasMoreElements()) {
            String key = iter.nextElement();
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        }
        if (includeParent && _parent != null) {
            _parent.getAllKeys(prefix, keys, includeParent);
        }
    }

    /**
     * Returns true if we have a translation mapping for the supplied key, false if not.
     */
    public boolean exists (String key)
    {
        return getResourceString(key, false) != null;
    }

    /**
     * Get a String from the resource bundle, or null if there was an error.
     */
    public String getResourceString (String key)
    {
        return getResourceString(key, true);
    }

    /**
     * Get a String from the resource bundle, or null if there was an error.
     *
     * @param key the resource key.
     * @param reportMissing whether or not the method should log an error if the resource didn't
     * exist.
     */
    public String getResourceString (String key, boolean reportMissing)
    {
        try {
            if (_bundle != null) {
                return _bundle.getString(key);
            }
        } catch (MissingResourceException mre) {
            // fall through and try the parent
        }

        // if we have a parent, try getting the string from them
        if (_parent != null) {
            String value = _parent.getResourceString(key, false);
            if (value != null) {
                return value;
            }
            // if we didn't find it in our parent, we want to fall
            // through and report missing appropriately
        }

        if (reportMissing) {
            log.warning("Missing translation message",
                        "bundle", _path, "key", key, new Exception());
        }

        return null;
    }

    /**
     * Obtains the translation for the specified message key. The specified arguments are
     * substituted into the translated string.
     *
     * <p> If the first argument in the array is an {@link Integer} object, a translation will be
     * selected accounting for plurality in the following manner. Assume a message key of
     * <code>m.widgets</code>, the following translations should be defined: <pre> m.widgets.0 =
     * no widgets. m.widgets.1 = {0} widget. m.widgets.n = {0} widgets. </pre>
     *
     * The specified argument is substituted into the translated string as appropriate. Consider
     * using:
     *
     * <pre> m.widgets.n = {0,number,integer} widgets. </pre>
     *
     * to obtain proper insertion of commas and dots as appropriate for the locale.
     *
     * <p> See {@link MessageFormat} for more information on how the substitution is performed. If
     * a translation message does not exist for the specified key, an error is logged and the key
     * itself (plus the arguments) is returned so that the caller need not worry about handling a
     * null response.
     */
    public String get (String key, Object... args)
    {
        // if this is a qualified key, we need to pass the buck to the
        // appropriate message bundle
        if (key.startsWith(MessageUtil.QUAL_PREFIX)) {
            MessageBundle qbundle = _msgmgr.getBundle(getBundle(key));
            return qbundle.get(getUnqualifiedKey(key), args);
        }

        // Select the proper suffix if our first argument can be coaxed into an integer
        String suffix = getSuffix(args);
        String msg = getResourceString(key + suffix, false);

        if (msg == null) {
            // Playing with fire: This only works because it's the same "" reference we return
            // from getSuffix()
            // Don't try this at home. Keep out of reach of children. If swallowed, consult
            // StringUtil.isBlank()
            if (suffix != "") {
                // Try the original key
                msg = getResourceString(key, false);
            }

            if (msg == null) {
                log.warning("Missing translation message", "bundle", _path, "key", key,
                    new Exception());

                // return something bogus
                return (key + StringUtil.toString(args));
            }
        }

        try {
            return MessageFormat.format(MessageUtil.escape(msg), args);

        } catch (IllegalArgumentException iae) {
            // The pattern is invalid or the arguments don't match up. Don't 'throw' because of
            // a translation error, we need to do our best for the user. But log it well.
            log.warning("Translation error: '" + iae.getMessage() + "'",
                "bundle", _path, "key", key, "msg", msg, "args", args, iae);
            return msg + StringUtil.toString(args);
        }
    }

    /**
     * Obtains the translation for the specified message key. The specified arguments are
     * substituted into the translated string.
     */
    public String get (String key, String... args)
    {
        return get(key, (Object[]) args);
    }

    /**
     * A helper function for {@link #get(String,Object[])} that allows us to automatically perform
     * plurality processing if our first argument can be coaxed to an {@link Integer}.
     */
    public String getSuffix (Object[] args)
    {
        if (args.length > 0 && args[0] != null) {
            try {
                int count = (args[0] instanceof Integer) ? (Integer)args[0] :
                    Integer.parseInt(args[0].toString());
                switch (count) {
                    case 0: return ".0";
                    case 1: return ".1";
                    default: return ".n";
                }
            } catch (NumberFormatException e) {
                // Fall out
            }
        }
        return "";
    }

    /**
     * Obtains the translation for the specified compound message key. A compound key contains the
     * message key followed by a tab separated list of message arguments which will be substituted
     * into the translation string.
     *
     * <p> See {@link MessageFormat} for more information on how the substitution is performed. If
     * a translation message does not exist for the specified key, an error is logged and the key
     * itself (plus the arguments) is returned so that the caller need not worry about handling a
     * null response.
     */
    public String xlate (String compoundKey)
    {
        // if this is a qualified key, we need to pass the buck to the appropriate message bundle;
        // we have to do it here because we want the compound arguments of this key to be
        // translated in the context of the containing message bundle qualification
        if (compoundKey.startsWith(MessageUtil.QUAL_PREFIX)) {
            MessageBundle qbundle = _msgmgr.getBundle(getBundle(compoundKey));
            return qbundle.xlate(getUnqualifiedKey(compoundKey));
        }

        // to be more efficient about creating unnecessary objects, we
        // do some checking before splitting
        int tidx = compoundKey.indexOf('|');
        if (tidx == -1) {
            return get(compoundKey);

        } else {
            String key = compoundKey.substring(0, tidx);
            String argstr = compoundKey.substring(tidx+1);
            String[] args = StringUtil.split(argstr, "|");
            // unescape and translate the arguments
            for (int ii = 0; ii < args.length; ii++) {
                // if the argument is tainted, do no further translation
                // (it might contain |s or other fun stuff)
                if (MessageUtil.isTainted(args[ii])) {
                    args[ii] = MessageUtil.unescape(MessageUtil.untaint(args[ii]));
                } else {
                    args[ii] = xlate(MessageUtil.unescape(args[ii]));
                }
            }
            return get(key, (Object[]) args);
        }
    }

    @Override
    public String toString ()
    {
        return "[bundle=" + _bundle + ", path=" + _path + "]";
    }

    /** The message manager via whom we'll resolve fully qualified translation strings. */
    protected MessageManager _msgmgr;

    /** The path that identifies the resource bundle we are using to obtain our messages. */
    protected String _path;

    /** The resource bundle from which we obtain our messages. */
    protected ResourceBundle _bundle;

    /** Our parent bundle if we're not the global bundle. */
    protected MessageBundle _parent;
}
