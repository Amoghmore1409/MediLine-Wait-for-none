const fs = require('fs');
const files = [
  'app/src/main/java/com/example/mediline/QueueManagementActivity.java',
  'app/src/main/java/com/example/mediline/ClinicDetailsActivity.java'
];

files.forEach(f => {
    let c = fs.readFileSync(f, 'utf8');
    c = c.replace(/'folder'/g, '"folder"');
    c = c.replace(/'Prescriptions'/g, '"Prescriptions"');
    c = c.replace(/'Records'/g, '"Records"');
    c = c.replace(/'secure_url'/g, '"secure_url"');
    c = c.replace(/'prescriptionUrl'/g, '"prescriptionUrl"');
    c = c.replace(/'Prescription uploaded successfully!'/g, '"Prescription uploaded successfully!"');
    c = c.replace(/'Failed to update Database!'/g, '"Failed to update Database!"');
    c = c.replace(/'Upload failed: '/g, '"Upload failed: "');
    c = c.replace(/'Upload Failed'/g, '"Upload Failed"');
    c = c.replace(/'Failed to upload medical record: '/g, '"Failed to upload medical record: "');
    fs.writeFileSync(f, c);
});
