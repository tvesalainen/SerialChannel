/*
 * Copyright (C) 2013 tkv
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

package org.vesalainen.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tkv
 */
public class LibraryLoader
{
    private static final Map<String,Path> map = new HashMap<>();
    
    public static void loadLibrary(Class<?> clazz, String lib) throws IOException
    {
        String osArch = System.getProperty("os.arch");
        if (osArch.contains("64"))
        {
            lib = lib+"64";
        }
        else
        {
            lib = lib+"32";
        }
        try
        {
            System.loadLibrary(lib);
        }
        catch (UnsatisfiedLinkError ule)
        {
            if (map.containsKey(lib))
            {
                return; // lib is already loaded
            }
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader == null)
            {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            String libraryName = System.mapLibraryName(lib);
            try (InputStream is = classLoader.getResourceAsStream(libraryName))
            {
                if (is == null)
                {
                    throw new UnsatisfiedLinkError(libraryName+" not found");
                }
                Path tempPath = Files.createTempFile(null, libraryName);
                Files.copy(is, tempPath, REPLACE_EXISTING);
                String path = tempPath.toString();
                System.load(path);
                System.err.println("Warning! Loading "+libraryName+" from "+path);
                System.err.println("Copy "+path+" to java.library.path as "+libraryName);
                map.put(lib, tempPath);
            }
        }
    }

}
