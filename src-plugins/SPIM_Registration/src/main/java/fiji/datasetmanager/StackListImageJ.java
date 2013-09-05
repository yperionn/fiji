package fiji.datasetmanager;

import mpicbg.spim.data.SpimData;

public class StackListImageJ extends StackList
{
	@Override
	public String getTitle() 
	{
		return "3d Image Stacks (ImageJ Opener)";
	}

	@Override
	public SpimData<?, ?> createDataset()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExtendedDescription()
	{
		return "This dataset definition supports a series of three-dimensional (3d) image stacks\n" +  
				 "all present in the same folder. The filename of each file must define timepoint,\n" +  
				 "angle, channel and illumination direction (or a subset of those).\n" + 
				 "The 3d image stacks must be stored as TIFF that can be opened by ImageJ natively.\n" + 
				 "\n" + 
				 "\n" + 
				 "The filenames of the 3d image stacks could be for example:\n" +
				 "\n" + 
				 "spim_TL1_Channel1_Illum1_Angle0.tif ... spim_TL100_Channel2_Illum2_Angle315.tif\n" + 
				 "data_TP01_Angle000.lsm ... data_TP70_Angle180.lsm\n" +
				 "Angle0.ome.tiff ... Angle288.ome.tiff\n" +
				 "\n" +
				 "Note: this definition can be used for OpenSPIM data if saved as plain TIFF.";
	}

}
