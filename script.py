import os
import logging
import subprocess
from datetime import datetime

# Функция для настройки логирования
def setup_logging(output_directory):
    log_file_path = os.path.join(output_directory, "script_log.txt")

    # Проверка существования файла логов. Если он существует, очищаем его.
    if os.path.exists(log_file_path):
        with open(log_file_path, 'w', encoding='utf-8') as log_file:
            log_file.truncate(0)  # Очищаем содержимое файла

    # Создаем два обработчика: один для логов в файл, второй для вывода в консоль
    file_handler = logging.FileHandler(log_file_path, 'a', 'utf-8')
    console_handler = logging.StreamHandler()

    # Формат логов
    log_format = '%(asctime)s - %(levelname)s - %(message)s'

    # Настройка логирования
    logging.basicConfig(
        level=logging.INFO,
        format=log_format,
        handlers=[file_handler, console_handler]  # Добавляем оба обработчика
    )

def collect_files(project_directory, output_directory, extensions=None):
    # Указываем расширения файлов, которые нужно собрать. Если не указано, берутся по умолчанию.
    if extensions is None:
        extensions = ['.java', '.json', '.fxml', '.css', '.svg', '.xml']

    # Получаем текущую дату в формате YYYY-MM-DD
    current_date = datetime.now().strftime("%Y-%m-%d")
    logging.info(f"Current date: {current_date}")

    # Определяем базовое имя выходного файла с учетом текущей даты
    base_output_file = f"combined_project_files_{current_date}"

    # Инициализируем переменную для версии файла, начнем с версии 1
    version = 1
    # Путь к файлу versions.txt, где хранятся данные о версиях
    version_file_path = os.path.join(output_directory, "versions.txt")

    # Проверяем, существует ли выходная директория, если нет — создаем её
    if not os.path.exists(output_directory):
        os.makedirs(output_directory)  # Создаем директорию, если её нет
        logging.info(f"Created output directory: {output_directory}")

    # Проверяем, существует ли файл versions.txt и обрабатываем его
    if os.path.exists(version_file_path):
        logging.info(f"Found versions.txt, processing existing versions.")
        with open(version_file_path, 'r', encoding='utf-8') as version_file:
            # Читаем все сохраненные версии из versions.txt
            versions = version_file.readlines()
            updated_versions = []  # Список для актуальных версий
            versions_exist = False  # Флаг, чтобы проверить, есть ли версия для текущей даты

            # Проходим по всем версиям и проверяем, существует ли файл для этой версии
            for line in versions:
                date, last_version = line.strip().split(',')  # Разделяем строку на дату и номер версии
                # Строим путь к файлу на основе версии
                file_path = os.path.join(output_directory, f"{base_output_file}_v{last_version}.java")

                # Если файл существует, сохраняем версию в список
                if os.path.exists(file_path):
                    updated_versions.append(line)
                    # Если версия для текущей даты найдена, увеличиваем её
                    if date == current_date:
                        version = int(last_version) + 1  # Увеличиваем номер версии
                        versions_exist = True

            # Перезаписываем versions.txt, оставляем только актуальные версии
            with open(version_file_path, 'w', encoding='utf-8') as version_file:
                version_file.writelines(updated_versions)
                logging.info(f"Updated versions in {version_file_path}. Removed outdated versions.")

    # Если для текущей даты нет записей, начинаем с версии 1
    if not versions_exist:
        logging.info(f"No previous version found for {current_date}. Starting with version 1.")

    # Формируем имя выходного файла для текущей версии
    output_file = os.path.join(output_directory, f"{base_output_file}_v{version}.java")
    logging.info(f"Creating output file: {output_file}")

    # Записываем текущую версию в versions.txt
    with open(version_file_path, 'a', encoding='utf-8') as version_file:
        version_file.write(f"{current_date},{version}\n")
        logging.info(f"Added new version {version} for {current_date} to {version_file_path}.")

    # Формируем имя текстового файла для сохранения путей и содержимого
    txt_output_file = os.path.join(output_directory, f"{base_output_file}_v{version}.txt")

    # Начинаем собирать и записывать файлы проекта в выходной файл
    try:
        with open(output_file, 'w', encoding='utf-8') as java_output, open(txt_output_file, 'w', encoding='utf-8') as txt_output:
            logging.info(f"Collecting files from {project_directory}...")
            for root, dirs, files in os.walk(project_directory):  # Проходим по всем файлам в проекте
                for file in files:
                    # Захватываем только файлы с указанными расширениями
                    if any(file.endswith(ext) for ext in extensions):
                        file_path = os.path.join(root, file)
                        java_output.write(f"// {file_path}\n")  # Записываем путь к файлу в выходной файл
                        with open(file_path, 'r', encoding='utf-8') as input_file:
                            file_content = input_file.read()
                            java_output.write(file_content)  # Записываем содержимое файла в выходной
                        java_output.write("\n\n")  # Добавляем разделитель между файлами

                        # Записываем путь и содержимое в текстовый файл
                        txt_output.write(f"// {file_path}\n")
                        txt_output.write(file_content)
                        txt_output.write("\n\n")

        # Логируем успешное завершение записи файлов (будет и в консоли, и в файле)
        logging.info(f"File {output_file} and {txt_output_file} have been successfully saved!")

        # Открываем папку, содержащую файл
        open_folder(output_file)
    except Exception as e:
        logging.error(f"Error while saving the file {output_file}: {e}")

# Функция для открытия папки, содержащей файл
def open_folder(file_path):
    folder_path = os.path.dirname(file_path)
    # Для Windows
    if os.name == 'nt':
        subprocess.run(['explorer', folder_path])
    # Для macOS
    elif os.name == 'posix':
        subprocess.run(['open', folder_path])
    # Для Linux
    elif os.name == 'posix':
        subprocess.run(['xdg-open', folder_path])

# Укажите путь к вашему проекту
project_dir = r"K:\Programm\IDEA Project\KoverG\KoverG"  # Замените на путь к вашему проекту
# Укажите путь к директории для выходных файлов
output_dir = r"K:\Programm\IDEA Project\KoverG\Version"  # Замените на путь к папке для версий

# Настроим логирование и создадим папку для логов (если необходимо)
setup_logging(output_dir)  # Логи будут записываться в файл "script_log.txt" в той же папке, где versions.txt

# Вызываем функцию сбора всех файлов
collect_files(project_dir, output_dir)
