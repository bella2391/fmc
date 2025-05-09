package net.kishax.mc.common.libs;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import net.kishax.mc.common.libs.interfaces.PackageManager;

public class JarLoader {

  private static final List<URLClassLoader> loaders = new ArrayList<>();

  public static void addLoader(URLClassLoader loader) {
    loaders.add(loader); // GCに回収されないように保持
  }

  public static CompletableFuture<Map<PackageManager, URLClassLoader>> makeURLClassLoaderFromJars(
      ClassLoader parentLoader, List<PackageManager> packages, Path dataDirectory) {
    Map<PackageManager, CompletableFuture<URLClassLoader>> futures = packages.stream()
        .collect(Collectors.toMap(
            pkg -> pkg,
            pkg -> {
              Path jarPath = PackageType.getTargetPath(pkg, dataDirectory);
              return CompletableFuture.supplyAsync(() -> {
                try {
                  URL jarurl = jarPath.toUri().toURL();
                  // 以降、依存関係のあるパッケージのURLClassLoaderを統一する
                  List<PackageManager> dependencies = pkg.getDependencies();
                  URL[] jarurls = new URL[dependencies.size() + 1];
                  jarurls[0] = jarurl;
                  if (!dependencies.isEmpty()) {
                    for (int i = 0; i < dependencies.size(); i++) {
                      PackageManager dep = dependencies.get(i);
                      Path depJarPath = PackageType.getTargetPath(dep, dataDirectory);
                      jarurls[i + 1] = depJarPath.toUri().toURL();
                    }
                  }

                  URLClassLoader urlClassLoader = new URLClassLoader(jarurls, parentLoader/* null */);
                  addLoader(urlClassLoader);
                  return urlClassLoader;
                } catch (IOException e) {
                  e.printStackTrace();
                  return null;
                }
              });
            }));
    return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().join())));
  }
}
