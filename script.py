import os
import logging
import subprocess
from datetime import datetime

def setup_logging(output_directory):
    service_directory = os.path.join(output_directory, "Service")
    if not os.path.exists(service_directory):
        os.makedirs(service_directory)
    log_file_path = os.path.join(service_directory, "script_log.txt")
    if os.path.exists(log_file_path):
        with open(log_file_path, 'w', encoding='utf-8') as log_file:
            log_file.truncate(0)
    file_handler = logging.FileHandler(log_file_path, 'a', 'utf-8')
    console_handler = logging.StreamHandler()
    log_format = '%(asctime)s - %(levelname)s - %(message)s'
    logging.basicConfig(
        level=logging.INFO,
        format=log_format,
        handlers=[file_handler, console_handler]
    )

def is_included(rel_path):
    rel_path_norm = rel_path.replace("\\", "/")
    # Черный список файлов
    excluded_files = {
        "src/main/resources/icon.ico",
        "src/main/resources/images/checkmark.svg",
        "src/main/resources/META-INF/MANIFEST.MF"
    }
    return (
            (rel_path_norm == "pom.xml"
             or rel_path_norm.startswith("data/")
             or rel_path_norm.startswith("src/"))
            and rel_path_norm not in excluded_files
    )

def read_file_safely(filename):
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            return f.read()
    except UnicodeDecodeError:
        try:
            with open(filename, 'r', encoding='cp1251') as f:
                return f.read()
        except UnicodeDecodeError:
            with open(filename, 'r', encoding='latin1') as f:
                return f.read()

def collect_files(project_directory, output_directory):
    current_date = datetime.now().strftime("%Y-%m-%d")
    base_output_file = f"combined_project_files_{current_date}"
    version = 1
    service_directory = os.path.join(output_directory, "Service")
    version_file_path = os.path.join(service_directory, "versions.txt")
    if not os.path.exists(service_directory):
        os.makedirs(service_directory)
    if os.path.exists(version_file_path):
        with open(version_file_path, 'r', encoding='utf-8') as version_file:
            versions = version_file.readlines()
            updated_versions = []
            versions_exist = False
            for line in versions:
                date, last_version = line.strip().split(',')
                file_path = os.path.join(output_directory, f"{base_output_file}_v{last_version}.java")
                if os.path.exists(file_path):
                    updated_versions.append(line)
                    if date == current_date:
                        version = int(last_version) + 1
                        versions_exist = True
            with open(version_file_path, 'w', encoding='utf-8') as version_file:
                version_file.writelines(updated_versions)
    else:
        with open(version_file_path, 'w', encoding='utf-8') as version_file:
            pass
        versions_exist = False
    if not versions_exist:
        logging.info(f"No previous version found for {current_date}. Starting with version 1.")

    output_file = os.path.join(output_directory, f"{base_output_file}_v{version}.java")
    txt_output_file = os.path.join(output_directory, f"{base_output_file}_v{version}.txt")
    with open(version_file_path, 'a', encoding='utf-8') as version_file:
        version_file.write(f"{current_date},{version}\n")
    try:
        collected_files = set()
        included_files = []
        # Сначала собираем список файлов, которые попадут в сборку
        for root, dirs, files in os.walk(project_directory):
            for file in files:
                full_path = os.path.join(root, file)
                rel_path = os.path.normpath(os.path.relpath(full_path, project_directory))
                if is_included(rel_path):
                    included_files.append(rel_path.replace("\\", "/"))

        # Запись итоговых файлов
        with open(output_file, 'w', encoding='utf-8') as java_output, open(txt_output_file, 'w', encoding='utf-8') as txt_output:
            # Список файлов в начале файла
            java_output.write("// В сборку включены следующие файлы:\n")
            txt_output.write("// В сборку включены следующие файлы:\n")
            for f in included_files:
                java_output.write(f"// {f}\n")
                txt_output.write(f"// {f}\n")
            java_output.write("\n\n")
            txt_output.write("\n\n")

            # Основной проход по файлам
            for root, dirs, files in os.walk(project_directory):
                for file in files:
                    full_path = os.path.join(root, file)
                    rel_path = os.path.normpath(os.path.relpath(full_path, project_directory))
                    if is_included(rel_path):
                        file_content = read_file_safely(full_path)
                        file_pair = (rel_path, file_content)
                        if file_pair not in collected_files:
                            collected_files.add(file_pair)
                            java_output.write(f"// {rel_path}\n")
                            java_output.write(file_content)
                            java_output.write("\n\n")
                            txt_output.write(f"// {rel_path}\n")
                            txt_output.write(file_content)
                            txt_output.write("\n\n")
                            logging.info(f"File {rel_path} has been successfully written to output.")
        logging.info(f"File {output_file} and {txt_output_file} have been successfully saved!")
        open_folder(output_file)
    except Exception as e:
        logging.error(f"Error while saving the file {output_file}: {e}")

def open_folder(file_path):
    folder_path = os.path.dirname(file_path)
    if os.name == 'nt':
        subprocess.run(['explorer', folder_path])
    elif os.name == 'posix':
        subprocess.run(['open', folder_path])
    elif os.name == 'posix':
        subprocess.run(['xdg-open', folder_path])

project_dir = r"K:\Programm\IDEA Project\KoverG\KoverG"
output_dir = r"K:\Programm\IDEA Project\KoverG\Version"

setup_logging(output_dir)
collect_files(project_dir, output_dir)
