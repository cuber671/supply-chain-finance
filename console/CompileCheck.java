import org.fisco.solc.compiler.SolidityCompiler;
import org.fisco.solc.compiler.Version;
import java.io.*;
import java.nio.file.*;

public class CompileCheck {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java CompileCheck <contract.sol>");
            return;
        }

        String contractFile = args[0];
        Path contractPath = Paths.get(contractFile).toAbsolutePath().getParent();
        System.out.println("Compiling: " + contractFile);
        System.out.println("Base path: " + contractPath);

        // Read the contract source
        byte[] source = Files.readAllBytes(Paths.get(contractFile));

        // Compile with base path for import resolution
        SolidityCompiler.Result result = SolidityCompiler.compile(
            source, false, false, Version.V0_8_26,
            new org.fisco.solc.compiler.SolidityCompiler.Option[]{}
        );

        if (!result.isFailed()) {
            String output = result.getOutput();
            System.out.println("\n=== Compilation Output ===");
            System.out.println(output);

            // Also check for errors/warnings
            String errors = result.getErrors();
            if (errors != null && !errors.isEmpty()) {
                System.out.println("\n=== Errors/Warnings ===");
                System.out.println(errors);
            }
        } else {
            System.err.println("Compilation failed:");
            System.err.println(result.getErrors());
        }
    }
}