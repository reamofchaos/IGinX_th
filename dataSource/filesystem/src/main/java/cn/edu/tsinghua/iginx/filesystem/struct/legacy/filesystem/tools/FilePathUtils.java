/*
 * IGinX - the polystore system with high performance
 * Copyright (C) Tsinghua University
 * TSIGinX@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package cn.edu.tsinghua.iginx.filesystem.struct.legacy.filesystem.tools;

import static cn.edu.tsinghua.iginx.filesystem.struct.legacy.filesystem.shared.Constant.*;

import cn.edu.tsinghua.iginx.auth.FilePermissionManager;
import cn.edu.tsinghua.iginx.auth.entity.FileAccessType;
import cn.edu.tsinghua.iginx.auth.utils.FilePermissionRuleNameFilters;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class FilePathUtils {

  public static File normalize(File file, FileAccessType... type) throws SecurityException {

    Predicate<String> ruleNameFilter = FilePermissionRuleNameFilters.filesystemRulesWithDefault();

    FilePermissionManager.Checker sourceChecker =
        FilePermissionManager.getInstance().getChecker(null, ruleNameFilter, type);

    Optional<Path> sourceCheckedPath = sourceChecker.normalize(file.getPath());
    if (!sourceCheckedPath.isPresent()) {
      throw new SecurityException("filesystem has no permission to access file: " + file);
    }
    return sourceCheckedPath.get().toFile();
  }

  public static String toIginxPath(String root, String storageUnit, String path) {
    if (path == null && storageUnit == null || storageUnit.equals(WILDCARD)) {
      return root;
    }
    // 之后根据规则修改获取文件名的方法， may fix it
    if (path == null) {
      return root + storageUnit;
    }
    if (path.equals(WILDCARD)) {
      return root + storageUnit + SEPARATOR + WILDCARD;
    }
    // TODO compaction 会调用 show columns，会错误地返回单级目录，导致传入的 path 不含 .
    if (!path.contains(".")) {
      return root + path;
    }
    String middlePath = path.substring(0, path.lastIndexOf("."));
    return root
        + storageUnit
        + SEPARATOR
        + middlePath.replace(".", SEPARATOR)
        + SEPARATOR
        + path.substring(path.lastIndexOf(".") + 1)
        + FILE_EXTENSION;
  }

  public static String toNormalFilePath(String root, String path) {
    if (path == null) {
      return root;
    }
    String[] parts = path.split("\\.");
    StringBuilder res = new StringBuilder();
    for (String s : parts) {
      s = s.replace("\\", ".");
      res.append(s).append(SEPARATOR);
    }
    res = new StringBuilder(res.substring(0, res.length() - 1));
    return root + res;
  }

  public static String convertAbsolutePathToPath(String root, String filePath, String storageUnit) {
    String tmp;
    // 对iginx文件操作
    if (filePath.contains(FILE_EXTENSION)) {
      tmp = filePath.substring(0, filePath.lastIndexOf(FILE_EXTENSION));
      if (storageUnit != null) {
        if (storageUnit.equals(WILDCARD)) {
          tmp = tmp.substring(tmp.indexOf(SEPARATOR, tmp.indexOf(root) + root.length() + 1) + 1);
        } else {
          tmp = tmp.substring(tmp.indexOf(storageUnit) + storageUnit.length() + 1);
        }
      } else {
        tmp = tmp.substring(tmp.indexOf(root) + root.length());
      }
      if (tmp.isEmpty()) {
        return SEPARATOR;
      }
      return tmp.replace(SEPARATOR, ".");
    } else { // 对普通文件操作
      String[] parts;
      tmp = filePath.substring(filePath.indexOf(root) + root.length());
      if (!tmp.contains(SEPARATOR)) { // 一级目录或文件
        return tmp.replace(".", "\\");
      }
      if (SEPARATOR.equals("\\")) { // 针对win系统
        parts = tmp.split("\\\\");
      } else {
        parts = tmp.split(SEPARATOR);
      }
      StringBuilder res = new StringBuilder();
      for (String s : parts) {
        s = s.replace(".", "\\");
        res.append(s).append(".");
      }
      return res.substring(0, res.length() - 1);
    }
  }

  public static String toFilePath(String root, String storageUnit, String path) {
    if (path == null) {
      return root;
    }
    StringBuilder target = new StringBuilder(root);
    if (storageUnit != null) {
      target.append(storageUnit).append(SEPARATOR);
    }
    String[] parts = path.split("\\.");
    StringBuilder res = new StringBuilder();
    for (String s : parts) {
      s = s.replace("\\", ".");
      res.append(s).append(SEPARATOR);
    }
    target.append(res.substring(0, res.length() - 1));
    if (storageUnit != null) {
      target.append(FILE_EXTENSION);
    }
    return target.toString();
  }

  public static boolean matches(Path path, List<String> regexList) {
    String filePath = path.toAbsolutePath().toString();
    for (String regex : regexList) {
      if (Pattern.matches(regex, filePath)) {
        return true;
      }
    }
    return false;
  }
}
