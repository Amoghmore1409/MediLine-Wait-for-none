const fs = require('fs');
const files = [
  'app/src/main/java/com/example/mediline/QueueManagementActivity.java',
  'app/src/main/java/com/example/mediline/ClinicDetailsActivity.java'
];

files.forEach(f => {
    let c = fs.readFileSync(f, 'utf8');
    c = c.replace(/import com\.google\.firebase\.storage\.FirebaseStorage;\r?\n/g, '');
    c = c.replace(/import com\.google\.firebase\.storage\.StorageReference;\r?\n/g, '');
    fs.writeFileSync(f, c);
});
