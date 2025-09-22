from flask import Flask, request, jsonify, Response
import firebase_admin
from firebase_admin import credentials, firestore
from jinja2 import Environment, FileSystemLoader, select_autoescape
import subprocess
import os
import tempfile
import shutil
import logging
import re
from typing import Dict, List, Any, Optional
from datetime import datetime

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# Initialize Firebase Admin SDK
try:
    # Try to initialize with default credentials (works on Cloud Run)
    firebase_admin.initialize_app()
    logger.info("Initialized Firebase with default credentials")
except Exception as e:
    logger.error(f"Failed to initialize Firebase: {e}")
    raise

db = firestore.client()

# Configure Jinja2
template_dir = os.path.dirname(os.path.abspath(__file__))
env = Environment(
    loader=FileSystemLoader(template_dir),
    autoescape=select_autoescape(['html', 'xml']),
    block_start_string='<%',
    block_end_string='%>',
    variable_start_string='<<',
    variable_end_string='>>',
    comment_start_string='<#',
    comment_end_string='#>'
)

def clean_filename(filename: str) -> str:
    """Clean filename for use in file system and headers."""
    # Remove or replace invalid characters
    cleaned = re.sub(r'[<>:"/\\|?*]', '_', filename)
    # Remove extra spaces and truncate
    cleaned = re.sub(r'\s+', ' ', cleaned.strip())
    if len(cleaned) > 50:
        cleaned = cleaned[:50]
    return cleaned or "cover_letter"

def escape_latex(text: str) -> str:
    """Escape special LaTeX characters in text."""
    if not isinstance(text, str):
        return str(text) if text is not None else ""
    
    # Dictionary of characters to escape
    latex_special_chars = {
        '&': r'\&',
        '%': r'\%',
        '$': r'\$',
        '#': r'\#',
        '^': r'\textasciicircum{}',
        '_': r'\_',
        '{': r'\{',
        '}': r'\}',
        '~': r'\textasciitilde{}',
        '\\': r'\textbackslash{}',
    }
    
    for char, escaped in latex_special_chars.items():
        text = text.replace(char, escaped)
    
    return text

def format_cover_letter_body(text: str) -> str:
    """Format cover letter body text for LaTeX with proper paragraph breaks."""
    if not text:
        return ""
    
    # First escape LaTeX special characters
    escaped_text = escape_latex(text)
    
    # Split into paragraphs (double line breaks or single line breaks)
    paragraphs = re.split(r'\n\s*\n', escaped_text.strip())
    
    # If no double line breaks found, split on single line breaks
    if len(paragraphs) == 1:
        paragraphs = escaped_text.strip().split('\n')
    
    # Clean up each paragraph and join with LaTeX paragraph breaks
    formatted_paragraphs = []
    for para in paragraphs:
        para = para.strip()
        if para:  # Only add non-empty paragraphs
            formatted_paragraphs.append(para)
    
    # Join paragraphs with double line breaks for LaTeX
    return '\n\n'.join(formatted_paragraphs)

def format_date(date_str: str) -> str:
    """Format date string for LaTeX."""
    if not date_str:
        return ""
    
    # If it's already a simple format, return as is
    if re.match(r'^\w+ \d{4}$', date_str):
        return date_str
    
    # Try to parse various date formats and convert to "Month Year"
    date_patterns = [
        r'^(\d{4})-(\d{1,2})-(\d{1,2})$',  # YYYY-MM-DD
        r'^(\d{1,2})/(\d{1,2})/(\d{4})$',   # MM/DD/YYYY or DD/MM/YYYY
        r'^(\d{1,2})-(\d{1,2})-(\d{4})$',   # MM-DD-YYYY or DD-MM-YYYY
    ]
    
    months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
              'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
    
    for pattern in date_patterns:
        match = re.match(pattern, date_str)
        if match:
            try:
                if pattern == date_patterns[0]:  # YYYY-MM-DD
                    year, month, day = match.groups()
                    return f"{months[int(month)-1]} {year}"
                else:  # MM/DD/YYYY or similar
                    part1, part2, year = match.groups()
                    # Assume first part is month if <= 12
                    month = int(part1) if int(part1) <= 12 else int(part2)
                    return f"{months[month-1]} {year}"
            except (ValueError, IndexError):
                continue
    
    # If no pattern matches, return the original
    return date_str

def get_cover_letter_data(user_id: str, cover_letter_id: str) -> Optional[Dict]:
    """Fetch cover letter data from Firestore."""
    try:
        doc_ref = db.collection('users').document(user_id).collection('coverLetters').document(cover_letter_id)
        doc = doc_ref.get()
        
        if not doc.exists:
            logger.error(f"Cover letter not found: {cover_letter_id} for user: {user_id}")
            return None
            
        cover_letter_data = doc.to_dict()
        logger.info(f"Retrieved cover letter data for: {cover_letter_id}")
        return cover_letter_data
        
    except Exception as e:
        logger.error(f"Error fetching cover letter data: {e}")
        return None

def validate_cover_letter_data(data: Dict) -> Dict:
    """Validate and format cover letter data."""
    validated_data = {}
    
    # Basic information
    validated_data['cover_letter_name'] = escape_latex(data.get('coverLetterName', ''))
    validated_data['sender_name'] = escape_latex(data.get('senderName', ''))
    validated_data['sender_email'] = escape_latex(data.get('senderEmail', ''))
    validated_data['sender_phone'] = escape_latex(data.get('senderPhoneNumber', ''))
    validated_data['sender_linkedin'] = escape_latex(data.get('senderLinkedInUrl', ''))
    validated_data['company'] = escape_latex(data.get('company', ''))
    validated_data['position'] = escape_latex(data.get('position', ''))
    
    # Format the body text with proper paragraph breaks
    validated_data['body'] = format_cover_letter_body(data.get('body', ''))
    
    # Format the date
    validated_data['date'] = datetime.now().strftime('%B %d, %Y')
    
    return validated_data

def generate_latex(template_name: str, data: Dict) -> Optional[str]:
    """Generate LaTeX code from template and data."""
    try:
        template_file = f"{template_name}.tex"
        template = env.get_template(template_file)
        
        latex_content = template.render(**data)
        logger.info(f"Generated LaTeX content using template: {template_name}")
        return latex_content
        
    except Exception as e:
        logger.error(f"Error generating LaTeX: {e}")
        return None

def compile_latex_to_pdf(latex_content: str) -> Optional[bytes]:
    """Compile LaTeX content to PDF."""
    with tempfile.TemporaryDirectory() as temp_dir:
        try:
            # Write LaTeX content to file
            tex_file = os.path.join(temp_dir, 'cover_letter.tex')
            with open(tex_file, 'w', encoding='utf-8') as f:
                f.write(latex_content)
            
            # Compile LaTeX to PDF
            result = subprocess.run([
                'pdflatex', 
                '-interaction=nonstopmode',
                '-output-directory', temp_dir,
                tex_file
            ], capture_output=True, text=True, cwd=temp_dir)
            
            if result.returncode != 0:
                logger.error(f"LaTeX compilation failed: {result.stderr}")
                logger.error(f"LaTeX output: {result.stdout}")
                return None
            
            # Read the generated PDF
            pdf_file = os.path.join(temp_dir, 'cover_letter.pdf')
            if os.path.exists(pdf_file):
                with open(pdf_file, 'rb') as f:
                    pdf_content = f.read()
                logger.info("Successfully compiled LaTeX to PDF")
                return pdf_content
            else:
                logger.error("PDF file was not generated")
                return None
                
        except Exception as e:
            logger.error(f"Error compiling LaTeX: {e}")
            return None

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    return jsonify({'status': 'healthy', 'service': 'cover-letter-service'})

@app.route('/generate-cover-letter', methods=['POST'])
def generate_cover_letter():
    """Generate cover letter PDF endpoint."""
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400
            
        user_id = data.get('user_id')
        cover_letter_id = data.get('cover_letter_id')
        
        if not user_id or not cover_letter_id:
            return jsonify({'error': 'user_id and cover_letter_id are required'}), 400
        
        # Fetch cover letter data
        cover_letter_data = get_cover_letter_data(user_id, cover_letter_id)
        if not cover_letter_data:
            return jsonify({'error': 'Cover letter not found'}), 404
        
        # Format data for template
        formatted_data = validate_cover_letter_data(cover_letter_data)
        if not formatted_data:
            return jsonify({'error': 'Failed to format cover letter data'}), 500
        
        # Use the cover letter template
        template_name = 'cover_letter_template'
        
        # Generate LaTeX
        latex_code = generate_latex(template_name, formatted_data)
        if not latex_code:
            return jsonify({'error': 'Failed to generate LaTeX'}), 500
        
        # Compile to PDF
        pdf_content = compile_latex_to_pdf(latex_code)
        if not pdf_content:
            return jsonify({'error': 'Failed to compile PDF'}), 500
        
        # Prepare response
        cover_letter_name = formatted_data.get('cover_letter_name', 'Cover_Letter')
        clean_name = clean_filename(cover_letter_name)
        
        response = Response(
            pdf_content,
            mimetype='application/pdf',
            headers={
                'Content-Disposition': f'attachment; filename="{clean_name}.pdf"',
                'Content-Type': 'application/pdf'
            }
        )
        return response
        
    except Exception as e:
        logger.error(f"Error generating cover letter: {e}")
        return jsonify({'error': 'Internal server error'}), 500

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 8080))
    app.run(host='0.0.0.0', port=port, debug=False)
