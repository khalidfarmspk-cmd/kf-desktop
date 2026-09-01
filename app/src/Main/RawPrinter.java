package Main;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;

/**
 * Sends raw bytes to a Windows printer using the Winspool RAW datatype
 * (bypasses the ZDesigner graphics driver so ZPL reaches the printer intact).
 */
public final class RawPrinter {

    private RawPrinter() {
    }

    public static void print(String printerName, byte[] data) throws Exception {
        if (printerName == null || printerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Printer name is empty");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Print data is empty");
        }

        File tmp = File.createTempFile("pos_raw_", ".bin");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tmp);
            fos.write(data);
            fos.close();
            fos = null;

            String psScript =
                    "$code = @'\n"
                    + "using System;\n"
                    + "using System.IO;\n"
                    + "using System.Runtime.InteropServices;\n"
                    + "public class RawPrinterHelper {\n"
                    + "  [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Ansi)]\n"
                    + "  public class DOCINFOA {\n"
                    + "    [MarshalAs(UnmanagedType.LPStr)] public string pDocName;\n"
                    + "    [MarshalAs(UnmanagedType.LPStr)] public string pOutputFile;\n"
                    + "    [MarshalAs(UnmanagedType.LPStr)] public string pDataType;\n"
                    + "  }\n"
                    + "  [DllImport(\"winspool.Drv\", EntryPoint=\"OpenPrinterA\", SetLastError=true, CharSet=CharSet.Ansi, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]\n"
                    + "  public static extern bool OpenPrinter([MarshalAs(UnmanagedType.LPStr)] string szPrinter, out IntPtr hPrinter, IntPtr pd);\n"
                    + "  [DllImport(\"winspool.Drv\", EntryPoint=\"ClosePrinter\", SetLastError=true, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]\n"
                    + "  public static extern bool ClosePrinter(IntPtr hPrinter);\n"
                    + "  [DllImport(\"winspool.Drv\", EntryPoint=\"StartDocPrinterA\", SetLastError=true, CharSet=CharSet.Ansi, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]\n"
                    + "  public static extern int StartDocPrinter(IntPtr hPrinter, int level, [In, MarshalAs(UnmanagedType.LPStruct)] DOCINFOA di);\n"
                    + "  [DllImport(\"winspool.Drv\", EntryPoint=\"EndDocPrinter\", SetLastError=true, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]\n"
                    + "  public static extern bool EndDocPrinter(IntPtr hPrinter);\n"
                    + "  [DllImport(\"winspool.Drv\", EntryPoint=\"StartPagePrinter\", SetLastError=true, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]\n"
                    + "  public static extern bool StartPagePrinter(IntPtr hPrinter);\n"
                    + "  [DllImport(\"winspool.Drv\", EntryPoint=\"EndPagePrinter\", SetLastError=true, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]\n"
                    + "  public static extern bool EndPagePrinter(IntPtr hPrinter);\n"
                    + "  [DllImport(\"winspool.Drv\", EntryPoint=\"WritePrinter\", SetLastError=true, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]\n"
                    + "  public static extern bool WritePrinter(IntPtr hPrinter, IntPtr pBytes, int dwCount, out int dwWritten);\n"
                    + "  public static bool SendBytes(string printerName, byte[] bytes) {\n"
                    + "    IntPtr hPrinter = IntPtr.Zero;\n"
                    + "    if (!OpenPrinter(printerName, out hPrinter, IntPtr.Zero)) return false;\n"
                    + "    DOCINFOA di = new DOCINFOA(); di.pDocName = \"POS Raw\"; di.pDataType = \"RAW\";\n"
                    + "    if (StartDocPrinter(hPrinter, 1, di) == 0) { ClosePrinter(hPrinter); return false; }\n"
                    + "    if (!StartPagePrinter(hPrinter)) { EndDocPrinter(hPrinter); ClosePrinter(hPrinter); return false; }\n"
                    + "    IntPtr p = Marshal.AllocCoTaskMem(bytes.Length);\n"
                    + "    Marshal.Copy(bytes, 0, p, bytes.Length);\n"
                    + "    int written = 0;\n"
                    + "    bool ok = WritePrinter(hPrinter, p, bytes.Length, out written);\n"
                    + "    Marshal.FreeCoTaskMem(p);\n"
                    + "    EndPagePrinter(hPrinter); EndDocPrinter(hPrinter); ClosePrinter(hPrinter);\n"
                    + "    return ok && written == bytes.Length;\n"
                    + "  }\n"
                    + "  public static bool SendFile(string printerName, string path) {\n"
                    + "    return SendBytes(printerName, File.ReadAllBytes(path));\n"
                    + "  }\n"
                    + "}\n"
                    + "'@\n"
                    + "Add-Type -TypeDefinition $code -Language CSharp\n"
                    + "$ok = [RawPrinterHelper]::SendFile('"
                    + escapePs(printerName)
                    + "', '"
                    + escapePs(tmp.getAbsolutePath())
                    + "')\n"
                    + "if (-not $ok) { throw 'RAW print failed for printer: " + escapePs(printerName) + "' }\n";

            File psFile = File.createTempFile("pos_raw_", ".ps1");
            FileOutputStream psOut = new FileOutputStream(psFile);
            try {
                psOut.write(psScript.getBytes(Charset.forName("UTF-8")));
            } finally {
                psOut.close();
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-File", psFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = readAll(p);
            int code = p.waitFor();
            psFile.delete();
            if (code != 0) {
                throw new Exception(output.trim().isEmpty()
                        ? ("RAW print failed, exit " + code)
                        : output.trim());
            }
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ignored) {
                }
            }
            tmp.delete();
        }
    }

    private static String escapePs(String s) {
        return s.replace("'", "''");
    }

    private static String readAll(Process p) throws Exception {
        java.io.InputStream in = p.getInputStream();
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[1024];
        int n;
        while ((n = in.read(b)) >= 0) {
            buf.write(b, 0, n);
        }
        return new String(buf.toByteArray(), Charset.forName("UTF-8"));
    }
}
