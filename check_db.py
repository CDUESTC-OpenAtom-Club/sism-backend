import subprocess, sys

try:
    import psycopg2
except ImportError:
    subprocess.check_call([sys.executable, "-m", "pip", "install", "psycopg2-binary", "-q"])
    import psycopg2

conn = psycopg2.connect(
    host="175.24.139.148",
    port=8386,
    database="strategic",
    user="postgres",
    password="64378561huaW"
)
cur = conn.cursor()

cur.execute("""
    SELECT tablename
    FROM pg_tables
    WHERE schemaname = 'public'
    ORDER BY tablename;
""")
tables = [row[0] for row in cur.fetchall()]

print("=== 数据库中所有表 ===")
for t in tables:
    print(f"  {t}")

print(f"\n总表数: {len(tables)}")

target_check = [
    "milestone", "assessment_cycle", "strategic_task", "strategic_task_backup",
    "sys_task", "cycle", "indicator_milestone"
]
print("\n=== 关键表存在性检查 ===")
for t in target_check:
    status = "✅ 存在" if t in tables else "❌ 不存在"
    print(f"  {t}: {status}")

cur.close()
conn.close()
