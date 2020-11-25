package com.sercomm.commons.id.util;

public class NodeNameUtil
{
    /**
     * Escapes the node portion of a JID according to "JID Escaping" (XEP-0106).
     * Escaping replaces characters prohibited by node-prep with escape sequences,
     * as follows:
     *
     * <table border="1" summary="Escaping rules">
     * <tr><td><b>Unescaped Character</b></td><td><b>Encoded Sequence</b></td></tr>
     * <tr><td>&lt;space&gt;</td><td>\20</td></tr>
     * <tr><td>"</td><td>\22</td></tr>
     * <tr><td>&amp;</td><td>\26</td></tr>
     * <tr><td>'</td><td>\27</td></tr>
     * <tr><td>/</td><td>\2f</td></tr>
     * <tr><td>:</td><td>\3a</td></tr>
     * <tr><td>&lt;</td><td>\3c</td></tr>
     * <tr><td>&gt;</td><td>\3e</td></tr>
     * <tr><td>@</td><td>\40</td></tr>
     * <tr><td>\</td><td>\5c</td></tr>
     * </table>
     *
     * This process is useful when the node comes from an external source that doesn't
     * conform to nodeprep. For example, a username in LDAP may be "Joe Smith". Because
     * the &lt;space&gt; character isn't a valid part of a node, the username should
     * be escaped to "Joe\20Smith" before being made into a JID (e.g. "joe\20smith@example.com"
     * after case-folding, etc. has been applied).
     *
     * All node escaping and un-escaping must be performed manually at the appropriate
     * time; the JID class will not escape or un-escape automatically.
     *
     * @param node the node.
     * @return the escaped version of the node.
     * @see <a href="http://xmpp.org/extensions/xep-0106.html">XEP-0106: JID Escaping</a>
     */
    public static String escapeNode(String node) {
        if (node == null) {
            return null;
        }
        final StringBuilder buf = new StringBuilder(node.length() + 8);
        for (int i = 0, n = node.length(); i < n; i++) {
            final char c = node.charAt(i);
            switch (c) {
                case '"':
                    buf.append("\\22");
                    break;
                case '&':
                    buf.append("\\26");
                    break;
                case '\'':
                    buf.append("\\27");
                    break;
                case '/':
                    buf.append("\\2f");
                    break;
                case ':':
                    buf.append("\\3a");
                    break;
                case '<':
                    buf.append("\\3c");
                    break;
                case '>':
                    buf.append("\\3e");
                    break;
                case '@':
                    buf.append("\\40");
                    break;
                case '\\':
                    final int c2 = (i + 1 < n) ? node.charAt(i + 1) : -1;
                    final int c3 = (i + 2 < n) ? node.charAt(i + 2) : -1;
                    if ((c2 == '2' && (c3 == '0' || c3 == '2' || c3 == '6' || c3 == '7' || c3 == 'f')) ||
                        (c2 == '3' && (c3 == 'a' || c3 == 'c' || c3 == 'e')) ||
                        (c2 == '4' && c3 == '0') ||
                        (c2 == '5' && c3 == 'c')) {
                        buf.append("\\5c");
                    } else {
                        buf.append(c);
                    }
                    break;
                default: {
                    if (Character.isWhitespace(c)) {
                        buf.append("\\20");
                    } else {
                        buf.append(c);
                    }
                }
            }
        }
        return buf.toString();
    }

    /**
     * Un-escapes the node portion of a JID according to "JID Escaping" (XEP-0106).
     * Escaping replaces characters prohibited by node-prep with escape sequences,
     * as follows:
     * <table border="1" summary="Escaped characters">
     * <tr><td><b>Unescaped Character</b></td><td><b>Encoded Sequence</b></td></tr>
     * <tr><td>&lt;space&gt;</td><td>\20</td></tr>
     * <tr><td>"</td><td>\22</td></tr>
     * <tr><td>&amp;</td><td>\26</td></tr>
     * <tr><td>'</td><td>\27</td></tr>
     * <tr><td>/</td><td>\2f</td></tr>
     * <tr><td>:</td><td>\3a</td></tr>
     * <tr><td>&lt;</td><td>\3c</td></tr>
     * <tr><td>&gt;</td><td>\3e</td></tr>
     * <tr><td>@</td><td>\40</td></tr>
     * <tr><td>\</td><td>\5c</td></tr>
     * </table>
     *
     * This process is useful when the node comes from an external source that doesn't
     * conform to nodeprep. For example, a username in LDAP may be "Joe Smith". Because
     * the &lt;space&gt; character isn't a valid part of a node, the username should
     * be escaped to "Joe\20Smith" before being made into a JID (e.g. "joe\20smith@example.com"
     * after case-folding, etc. has been applied).
     *
     * All node escaping and un-escaping must be performed manually at the appropriate
     * time; the JID class will not escape or un-escape automatically.
     *
     * @param node the escaped version of the node.
     * @return the un-escaped version of the node.
     * @see <a href="http://xmpp.org/extensions/xep-0106.html">XEP-0106: JID Escaping</a>
     */
    public static String unescapeNode(String node) {
        if (node == null) {
            return null;
        }
        char[] nodeChars = node.toCharArray();
        StringBuilder buf = new StringBuilder(nodeChars.length);
        for (int i = 0, n = nodeChars.length; i < n; i++) {
            compare:
            {
                char c = node.charAt(i);
                if (c == '\\' && i + 2 < n) {
                    char c2 = nodeChars[i + 1];
                    char c3 = nodeChars[i + 2];
                    if (c2 == '2') {
                        switch (c3) {
                            case '0':
                                buf.append(' ');
                                i += 2;
                                break compare;
                            case '2':
                                buf.append('"');
                                i += 2;
                                break compare;
                            case '6':
                                buf.append('&');
                                i += 2;
                                break compare;
                            case '7':
                                buf.append('\'');
                                i += 2;
                                break compare;
                            case 'f':
                                buf.append('/');
                                i += 2;
                                break compare;
                        }
                    } else if (c2 == '3') {
                        switch (c3) {
                            case 'a':
                                buf.append(':');
                                i += 2;
                                break compare;
                            case 'c':
                                buf.append('<');
                                i += 2;
                                break compare;
                            case 'e':
                                buf.append('>');
                                i += 2;
                                break compare;
                        }
                    } else if (c2 == '4') {
                        if (c3 == '0') {
                            buf.append("@");
                            i += 2;
                            break compare;
                        }
                    } else if (c2 == '5') {
                        if (c3 == 'c') {
                            buf.append("\\");
                            i += 2;
                            break compare;
                        }
                    }
                }
                buf.append(c);
            }
        }
        return buf.toString();
    }
}
