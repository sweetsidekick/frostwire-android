/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.search.extractors.js;

import static com.frostwire.search.extractors.js.JavaFunctions.isalpha;
import static com.frostwire.search.extractors.js.JavaFunctions.isdigit;
import static com.frostwire.search.extractors.js.JavaFunctions.join;
import static com.frostwire.search.extractors.js.JavaFunctions.len;
import static com.frostwire.search.extractors.js.JavaFunctions.list;
import static com.frostwire.search.extractors.js.JavaFunctions.reverse;
import static com.frostwire.search.extractors.js.JavaFunctions.splice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class JsFunction<T> {

    private final String jscode;
    private final Map<String, LambdaN> functions;
    private final LambdaN initial_function;

    public JsFunction(String jscode, String funcname) {
        this.jscode = jscode;
        this.functions = new HashMap<String, LambdaN>();
        this.initial_function = extract_function(funcname);
    }

    @SuppressWarnings("unchecked")
    public T eval(Object[] args) {
        return (T) initial_function.eval(args);
    }

    public T eval(Object s) {
        return eval(new Object[] { s });
    }

    private Object interpret_statement(String stmt, final Map<String, Object> local_vars, final int allow_recursion) {
        if (allow_recursion < 0) {
            throw new JsError("Recursion limit reached");
        }

        if (stmt.startsWith("var ")) {
            stmt = stmt.substring("var ".length());
        }

        final int outIdx = 1;
        final int indexIdx = 2;
        final int exprIdx = 3;
        //final Matcher ass_m = Pattern.compile("^(?<out>[a-z]+)(\\[(?<index>.+?)\\])?=(?<expr>.*)$").matcher(stmt);
        final Matcher ass_m = Pattern.compile("^([a-z]+)(\\[(.+?)\\])?=(.*)$").matcher(stmt);
        Lambda1 assign;
        String expr;
        if (ass_m.find()) {
            if (ass_m.group(indexIdx) != null) {
                assign = new Lambda1() {
                    @Override
                    public Object eval(Object val) {
                        Object lvar = local_vars.get(ass_m.group(outIdx));
                        Object idx = interpret_expression(ass_m.group(indexIdx), local_vars, allow_recursion);
                        assert idx instanceof Integer;
                        ((Object[]) lvar)[(Integer) idx] = val;
                        return val;
                    }
                };
                expr = ass_m.group(exprIdx);
            } else {
                assign = new Lambda1() {
                    @Override
                    public Object eval(Object val) {
                        local_vars.put(ass_m.group(outIdx), val);
                        return val;
                    }
                };
                expr = ass_m.group(exprIdx);
            }
        } else if (stmt.startsWith("return ")) {
            assign = new Lambda1() {
                @Override
                public Object eval(Object v) {
                    return v;
                }
            };
            expr = stmt.substring("return ".length());
        } else {
            throw new JsError(String.format("Cannot determine left side of statement in %s", stmt));
        }

        Object v = interpret_expression(expr, local_vars, allow_recursion);
        return assign.eval(v);
    }

    private Object interpret_expression(String expr, Map<String, Object> local_vars, int allow_recursion) {
        if (isdigit(expr)) {
            return Integer.valueOf(expr);
        }

        if (isalpha(expr)) {
            return local_vars.get(expr);
        }

        int inIdx = 1;
        int memberIdx = 2;
        //final Matcher m = Pattern.compile("^(?<in>[a-z]+)\\.(?<member>.*)$").matcher(expr);
        Matcher m = Pattern.compile("^([a-z]+)\\.(.*)$").matcher(expr);
        if (m.find()) {
            String member = m.group(memberIdx);
            Object val = local_vars.get(m.group(inIdx));
            if (member.equals("split(\"\")")) {
                return list((String) val);
            }
            if (member.equals("join(\"\")")) {
                return join((Object[]) val);
            }
            if (member.equals("length")) {
                return len(val);
            }
            if (member.equals("reverse()")) {
                return reverse(val);
            }

            final int idxIdx = 1;
            //final Matcher slice_m = Pattern.compile("slice\\((?<idx>.*)\\)").matcher(member);
            final Matcher slice_m = Pattern.compile("slice\\((.*)\\)").matcher(member);
            if (slice_m.find()) {
                Object idx = interpret_expression(slice_m.group(idxIdx), local_vars, allow_recursion - 1);
                return splice(val, (Integer) idx);
            }
        }

        inIdx = 1;
        int idxIdx = 2;
        //m = Pattern.compile("^(?<in>[a-z]+)\\[(?<idx>.+)\\]$").matcher(expr);
        m = Pattern.compile("^([a-z]+)\\[(.+)\\]$").matcher(expr);
        if (m.find()) {
            Object val = local_vars.get(m.group(inIdx));
            Object idx = interpret_expression(m.group(idxIdx), local_vars, allow_recursion - 1);
            return ((Object[]) val)[(Integer) idx];
        }

        int aIdx = 1;
        int bIdx = 3;
        //m = Pattern.compile("^(?<a>.+?)(?<op>[%])(?<b>.+?)$").matcher(expr);
        m = Pattern.compile("^(.+?)([%])(.+?)$").matcher(expr);
        if (m.find()) {
            Object a = interpret_expression(m.group(aIdx), local_vars, allow_recursion);
            Object b = interpret_expression(m.group(bIdx), local_vars, allow_recursion);
            return (Integer) a % (Integer) b;
        }

        int funcIdx = 1;
        int argsIdx = 2;
        //m = Pattern.compile("^(?<func>[a-zA-Z]+)\\((?<args>[a-z0-9,]+)\\)$").matcher(expr);
        m = Pattern.compile("^([a-zA-Z]+)\\(([a-z0-9,]+)\\)$").matcher(expr);
        if (m.find()) {
            String fname = m.group(funcIdx);
            if (!functions.containsKey(fname)) {
                functions.put(fname, extract_function(fname));
            }
            List<Object> argvals = new ArrayList<Object>();
            for (String v : m.group(argsIdx).split(",")) {
                if (isdigit(v)) {
                    argvals.add(Integer.valueOf(v));
                } else {
                    argvals.add(local_vars.get(v));
                }
            }
            return functions.get(fname).eval(argvals.toArray());
        }
        throw new JsError(String.format("Unsupported JS expression %s", expr));
    }

    private LambdaN extract_function(String funcname) {
        int argsIdx = 1;
        final int codeIdx = 2;
        //final Matcher func_m = Pattern.compile("function " + Pattern.quote(funcname) + "\\((?<args>[a-z,]+)\\)\\{(?<code>[^\\}]+)\\}").matcher(jscode);
        final Matcher func_m = Pattern.compile("function " + Pattern.quote(funcname) + "\\(([a-z,]+)\\)\\{([^\\}]+)\\}").matcher(jscode);
        func_m.find();
        final String[] argnames = func_m.group(argsIdx).split(",");

        LambdaN resf = new LambdaN() {
            @Override
            public Object eval(Object[] args) {
                Map<String, Object> local_vars = new HashMap<String, Object>();
                for (int i = 0; i < argnames.length; i++) {
                    local_vars.put(argnames[i], args[i]);
                }
                Object res = null;
                for (String stmt : func_m.group(codeIdx).split(";")) {
                    res = interpret_statement(stmt, local_vars, 20);
                }
                return res;
            }
        };

        return resf;
    }
}
