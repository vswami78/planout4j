package com.glassdoor.planout4j.tools;

import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.nocrala.tools.texttablefmt.BorderStyle;
import org.nocrala.tools.texttablefmt.CellStyle;
import org.nocrala.tools.texttablefmt.ShownBorders;
import org.nocrala.tools.texttablefmt.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import com.google.gson.Gson;

import com.glassdoor.planout4j.NamespaceConfig;
import com.glassdoor.planout4j.config.Planout4jRepositoryImpl;
import com.glassdoor.planout4j.config.ValidationException;

/**
 * Command-line interface for getting information about namespaces in the target backend.
 * Supports listing all namespaces (with short summary), filtering by name pattern,
 * and displaying full details.
 */
public class NslistTool {

    private static final Logger LOG = LoggerFactory.getLogger(NslistTool.class);

    public static void execute(final Namespace parsedArgs) throws IOException, ValidationException {
        final Map<String, NamespaceConfig> namespaces = new Planout4jRepositoryImpl().loadAllNamespaceConfigs();
        final boolean fullDeails = parsedArgs.getBoolean("full");
        final Table table = new Table(5, BorderStyle.CLASSIC, ShownBorders.ALL);
        addCells(table, "name", "total segs", "used segs", "definitions", "active experiments");
        final Gson gson = Planout4jTool.getGson(parsedArgs);

        final String namePatternStr = StringUtils.lowerCase(parsedArgs.getString("name"));
        Pattern namePattern = null;
        try {
            if (namePatternStr != null && !StringUtils.isAlphanumeric(namePatternStr)) {
                LOG.debug("name pattern '{}' is not alphanumeric, assuming a regex", namePatternStr);
                namePattern = Pattern.compile(namePatternStr, Pattern.CASE_INSENSITIVE);
            }
        } catch (PatternSyntaxException e) {
            LOG.warn("Invalid name regex, listing all namespace", e);
        }

        for (String name : new TreeSet<>(namespaces.keySet())) {
            NamespaceConfig nsConf = namespaces.get(name);
            if (namePatternStr == null
                    || namePattern != null && namePattern.matcher(name).matches()
                    || name.toLowerCase().contains(namePatternStr))
            {
                if (fullDeails) {
                    System.out.printf("********************** START of %s *********************\n", name);
                    System.out.println(gson.toJson(nsConf.getConfig()));
                    System.out.printf("*********************** END of %s **********************\n", name);
                } else {
                    addCells(table, name, nsConf.getTotalSegments(), nsConf.getUsedSegments(),
                            nsConf.getExperimentDefsCount(), nsConf.getActiveExperimentsCount());
                }
            } else {
                LOG.trace("namespace name {} doesn't match pattern {}", name, namePatternStr);
            }
        }
        if (!fullDeails) {
            System.out.println(table.render());
        }
    }

    private static void addCells(final Table table, final Object... cells) {
        for (Object cell : cells) {
            table.addCell(cell.toString(), new CellStyle(
                    cell instanceof Number ? CellStyle.HorizontalAlign.right : CellStyle.HorizontalAlign.left));
        }
    }

    private NslistTool() {}

}
