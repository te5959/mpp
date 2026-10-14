import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;
import net.sf.mpxj.reader.UniversalProjectReader;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Desktop;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class MppViewer {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) throws Exception {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        File last = null;
        if (args.length > 0) last = new File(args[0]);

        while (true) {
            File chosen = last;
            last = null;
            if (chosen == null) {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Choose a Microsoft Project file");
                fc.setFileFilter(new FileNameExtensionFilter(
                        "Project files (mpp, mpt, xml, mpx, xer)", "mpp", "mpt", "xml", "mpx", "xer"));
                if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
                chosen = fc.getSelectedFile();
            }

            try {
                ProjectFile project = new UniversalProjectReader().read(chosen);
                if (project == null) {
                    JOptionPane.showMessageDialog(null, "This file format is not supported.",
                            "MPP Viewer", JOptionPane.ERROR_MESSAGE);
                } else {
                    String html = buildHtml(project, chosen.getName());
                    Path out = Files.createTempFile("mpp-view-", ".html");
                    Files.write(out, html.getBytes(StandardCharsets.UTF_8));
                    out.toFile().deleteOnExit();
                    Desktop.getDesktop().browse(out.toUri());
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Could not open the file:\n" + e,
                        "MPP Viewer", JOptionPane.ERROR_MESSAGE);
            }

            int again = JOptionPane.showConfirmDialog(null, "Open another file?",
                    "MPP Viewer", JOptionPane.YES_NO_OPTION);
            if (again != JOptionPane.YES_OPTION) {
                // give the browser a moment to load the temp file before it is deleted
                Thread.sleep(4000);
                return;
            }
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String buildHtml(ProjectFile project, String fileName) {
        List<Task> tasks = new ArrayList<>();
        LocalDateTime min = null, max = null;
        for (Task t : project.getTasks()) {
            if (t.getID() != null && t.getID() == 0) continue;      // hidden project-summary row
            if (t.getName() == null || t.getStart() == null || t.getFinish() == null) continue;
            tasks.add(t);
            if (min == null || t.getStart().isBefore(min)) min = t.getStart();
            if (max == null || t.getFinish().isAfter(max)) max = t.getFinish();
        }
        double total = 1;
        if (min != null) total = Math.max(1, Duration.between(min, max).toMinutes());

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>")
          .append(esc(fileName)).append("</title><style>")
          .append("body{font:13px Segoe UI,Arial,sans-serif;margin:0;background:#fff;color:#222}")
          .append("header{padding:12px 16px;background:#0f766e;color:#fff}")
          .append("header h1{margin:0;font-size:18px}header small{opacity:.85}")
          .append("#bar{padding:8px 16px;background:#f3f4f6;border-bottom:1px solid #ddd}")
          .append("#bar input{padding:5px 8px;width:260px}")
          .append("table{border-collapse:collapse;width:100%}")
          .append("th{position:sticky;top:0;background:#e5e7eb;text-align:left;padding:5px 8px;border-bottom:1px solid #bbb;white-space:nowrap}")
          .append("td{padding:3px 8px;border-bottom:1px solid #eee;white-space:nowrap}")
          .append("tr.sum td{font-weight:bold;background:#f9fafb}")
          .append("td.g{width:45%;min-width:380px;position:relative;padding:0}")
          .append(".track{position:relative;height:20px}")
          .append(".b{position:absolute;top:5px;height:10px;background:#3b82f6;border-radius:2px;overflow:hidden}")
          .append(".b i{display:block;height:100%;background:#1e3a8a}")
          .append(".b.c{background:#f87171}.b.c i{background:#991b1b}")
          .append(".b.s{background:#374151;height:6px;top:7px}.b.s i{background:#111}")
          .append(".m{position:absolute;top:4px;width:12px;height:12px;background:#111;transform:rotate(45deg);margin-left:-6px}")
          .append("</style></head><body>");
        sb.append("<header><h1>").append(esc(fileName)).append("</h1><small>")
          .append(tasks.size()).append(" tasks");
        if (min != null) sb.append(" &middot; ").append(FMT.format(min)).append(" &rarr; ").append(FMT.format(max));
        sb.append("</small></header>")
          .append("<div id='bar'><input id='q' placeholder='Search tasks or resources...' oninput='f()'> ")
          .append("<label><input type='checkbox' id='crit' onchange='f()' style='width:auto'> Critical only</label></div>")
          .append("<table id='t'><thead><tr><th>ID</th><th>WBS</th><th>Task</th><th>Duration</th><th>Start</th>")
          .append("<th>Finish</th><th>% done</th><th>Resources</th><th>Gantt</th></tr></thead><tbody>");

        for (Task t : tasks) {
            boolean sum = Boolean.TRUE.equals(t.getSummary());
            boolean ms = Boolean.TRUE.equals(t.getMilestone());
            boolean crit = Boolean.TRUE.equals(t.getCritical());
            Number pn = t.getPercentageComplete();
            double pc = pn == null ? 0 : pn.doubleValue();
            int lvl = t.getOutlineLevel() == null ? 1 : t.getOutlineLevel();

            StringJoiner res = new StringJoiner(", ");
            for (ResourceAssignment ra : t.getResourceAssignments()) {
                if (ra.getResource() != null && ra.getResource().getName() != null)
                    res.add(ra.getResource().getName());
            }

            double left = Duration.between(min, t.getStart()).toMinutes() / total * 100;
            double width = Math.max(0.4, Duration.between(t.getStart(), t.getFinish()).toMinutes() / total * 100);

            sb.append("<tr class='").append(sum ? "sum" : "").append("' data-c='").append(crit ? 1 : 0).append("'>")
              .append("<td>").append(t.getID()).append("</td>")
              .append("<td>").append(esc(t.getWBS())).append("</td>")
              .append("<td style='padding-left:").append(8 + (lvl - 1) * 16).append("px'>")
              .append(esc(t.getName())).append("</td>")
              .append("<td>").append(t.getDuration() == null ? "" : esc(t.getDuration().toString())).append("</td>")
              .append("<td>").append(FMT.format(t.getStart())).append("</td>")
              .append("<td>").append(FMT.format(t.getFinish())).append("</td>")
              .append("<td>").append(Math.round(pc)).append("%</td>")
              .append("<td>").append(esc(res.toString())).append("</td>")
              .append("<td class='g'><div class='track'>");
            if (ms) {
                sb.append(String.format(java.util.Locale.ROOT, "<div class='m' style='left:%.3f%%'></div>", left));
            } else {
                sb.append(String.format(java.util.Locale.ROOT,
                        "<div class='b%s%s' style='left:%.3f%%;width:%.3f%%'><i style='width:%.0f%%'></i></div>",
                        sum ? " s" : "", crit ? " c" : "", left, width, Math.min(100, pc)));
            }
            sb.append("</div></td></tr>");
        }

        sb.append("</tbody></table><script>")
          .append("function f(){var q=document.getElementById('q').value.toLowerCase();")
          .append("var c=document.getElementById('crit').checked;")
          .append("document.querySelectorAll('#t tbody tr').forEach(function(r){")
          .append("var ok=r.textContent.toLowerCase().indexOf(q)>-1&&(!c||r.dataset.c==='1');")
          .append("r.style.display=ok?'':'none';});}")
          .append("</script></body></html>");
        return sb.toString();
    }
}
