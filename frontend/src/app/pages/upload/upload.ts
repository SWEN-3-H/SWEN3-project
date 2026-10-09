import { Component } from '@angular/core';

@Component({
  selector: 'app-upload',
  imports: [],
  templateUrl: './upload.html',
  styleUrl: './upload.css',
})
export class Upload {
  files: File[] =[];
  isHovering: boolean = false;

  dragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isHovering = true;
  }

  dragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isHovering = false;
  }

  drop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isHovering = false;

    if(event.dataTransfer && event.dataTransfer.files.length > 0) {
      this.handleFiles(event.dataTransfer.files);
    }
  }

  private handleFiles(fileList: FileList) {
    for(let i = 0; i < fileList.length; i++) {
      this.files.push(fileList.item(i)!);
    }
  }

  fileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if(input.files && input.files.length > 0) {
      this.handleFiles(input.files);
    }
  }

  removeFile(index: number) {
    this.files.splice(index, 1);
  }

  upload(){
    console.log("Ready to send to Backend", this.files);
  }
}
