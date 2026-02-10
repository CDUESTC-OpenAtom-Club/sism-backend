import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Utility to migrate Org references to SysOrg in Java files
 * Run with: java MigrateOrgToSysOrg.java
 */
public class MigrateOrgToSysOrg {
    
    public static void main(String[] args) throws IOException {
        Path srcMain = Paths.get("src/main/java");
        Path srcTest = Paths.get("src/test/java");
        
        System.out.println("🔄 Starting Org to SysOrg migration...\n");
        
        int mainCount = processDirectory(srcMain);
        int testCount = processDirectory(srcTest);
        
        System.out.println("\n✅ Migration completed!");
        System.out.println("📊 Updated " + mainCount + " main files");
        System.out.println("📊 Updated " + testCount + " test files");
        System.out.println("\n⚠️  Please review changes and run: mvn clean test");
    }
    
    private static int processDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return 0;
        }
        
        List<Path> javaFiles = Files.walk(dir)
            .filter(p -> p.toString().endsWith(".java"))
            .filter(p -> {
                try {
                    String content = Files.readString(p);
                    return content.contains("import com.sism.entity.Org;") ||
                           content.contains("OrgRepository") ||
                           content.contains("OrgService");
                } catch (IOException e) {
                    return false;
                }
            })
            .collect(Collectors.toList());
        
        for (Path file : javaFiles) {
            processFile(file);
        }
        
        return javaFiles.size();
    }
    
    private static void processFile(Path file) throws IOException {
        System.out.println("📝 Processing: " + file);
        
        String content = Files.readString(file);
        String original = content;
        
        // Replace imports
        content = content.replace("import com.sism.entity.Org;", "import com.sism.entity.SysOrg;");
        content = content.replace("import com.sism.repository.OrgRepository;", "import com.sism.repository.SysOrgRepository;");
        content = content.replace("import com.sism.service.OrgService;", "import com.sism.service.SysOrgService;");
        content = content.replace("import com.sism.vo.OrgVO;", "import com.sism.vo.SysOrgVO;");
        
        // Replace type declarations (careful with word boundaries)
        content = content.replaceAll("\\bOrg\\s+([a-z][a-zA-Z0-9]*)\\b", "SysOrg $1");
        content = content.replaceAll("\\bOrgRepository\\s+([a-z][a-zA-Z0-9]*)\\b", "SysOrgRepository $1");
        content = content.replaceAll("\\bOrgService\\s+([a-z][a-zA-Z0-9]*)\\b", "SysOrgService $1");
        content = content.replaceAll("\\bOrgVO\\s+([a-z][a-zA-Z0-9]*)\\b", "SysOrgVO $1");
        
        // Replace method calls
        content = content.replace(".getOrgId()", ".getId()");
        content = content.replace(".setOrgId(", ".setId(");
        content = content.replace(".getOrgName()", ".getName()");
        content = content.replace(".setOrgName(", ".setName(");
        content = content.replace(".getOrgType()", ".getType()");
        content = content.replace(".setOrgType(", ".setType(");
        
        // Replace JPA query paths
        content = content.replace(".org.orgId", ".org.id");
        content = content.replace(".org.orgName", ".org.name");
        content = content.replace(".org.orgType", ".org.type");
        content = content.replace("Org_OrgId", "Org_Id");
        content = content.replace("findByOrg_OrgId", "findByOrg_Id");
        
        // Replace generic references in annotations and generics
        content = content.replaceAll("<Org>", "<SysOrg>");
        content = content.replaceAll("<Org,", "<SysOrg,");
        content = content.replaceAll("\\(Org\\)", "(SysOrg)");
        
        if (!content.equals(original)) {
            Files.writeString(file, content);
            System.out.println("   ✅ Updated");
        } else {
            System.out.println("   ⏭️  No changes needed");
        }
    }
}
